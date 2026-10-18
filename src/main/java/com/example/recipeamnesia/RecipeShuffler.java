package com.example.recipeamnesia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RecipeShuffler {
    public enum PinResult { OK, UNKNOWN, ALREADY_PINNED }

    /** The RecipeManager we captured the originals from. A new instance appears on every (re)load of datapacks. */
    private static RecipeManager tracked;
    private static final Map<ResourceLocation, RecipeHolder<?>> HOLDERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ItemStack> ORIGINALS = new LinkedHashMap<>();

    // ------------------------------------------------------------------ tick

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld() == null) return;

        RecipeManager manager = server.getRecipeManager();
        ShuffleState state = ShuffleState.get(server);

        if (manager != tracked) {
            // server start or /reload: recipes are fresh (original) objects again
            tracked = manager;
            capture(server, manager);
            DropRandomizer.invalidate();
            DropRandomizer.seed = state.dropSeed;
            if (state.current.isEmpty()) {
                shuffle(server, false);        // brand-new world: start already shuffled
            } else {
                applyAll(state);               // restore the saved state
                syncToPlayers(server);
            }
            return;
        }

        int interval = Config.intervalTicks();
        if (state.ticksLeft <= 0) state.ticksLeft = interval; // (no upper clamp: amulets may extend the iteration)
        state.ticksLeft--;

        if (state.ticksLeft <= 0) {
            shuffle(server, true);
        } else {
            if (Config.ANNOUNCE.get() && state.ticksLeft == 200) {
                server.getPlayerList().broadcastSystemMessage(Component.translatable("message.recipeamnesia.warning"), true);
            }
            if (state.ticksLeft % 200 == 0) state.setDirty();
        }
    }

    // --------------------------------------------------------------- capture

    private static void capture(MinecraftServer server, RecipeManager manager) {
        HOLDERS.clear();
        ORIGINALS.clear();
        Config.RecipeFilters filters = Config.recipeFilters();
        for (RecipeHolder<?> holder : manager.getOrderedRecipes()) {
            if (RecipeAccess.isShufflable(holder, server.registryAccess(), filters)) {
                HOLDERS.put(holder.id(), holder);
                ORIGINALS.put(holder.id(), RecipeAccess.getResult(holder.value()).copy());
            }
        }
        ItemPools.rebuild(server, manager);
    }

    public static int shufflableCount() {
        return HOLDERS.size();
    }

    // --------------------------------------------------------------- shuffle

    /** Re-deals every recipe, picks a new drop seed, re-syncs clients and restarts the timer. */
    public static void shuffle(MinecraftServer server, boolean announce) {
        ShuffleState state = ShuffleState.get(server);
        Random random = new Random();

        // group recipes: everything together (default) or one group per recipe category/station
        Map<String, List<ResourceLocation>> groups = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, RecipeHolder<?>> e : HOLDERS.entrySet()) {
            String key = Config.SHUFFLE_WITHIN_TYPE.get() ? RecipeAccess.categoryOf(e.getValue().value()) : "all";
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(e.getKey());
        }

        List<Item> uncraftable = ItemPools.uncraftable;
        int extras = Config.UNCRAFTABLE_RECIPES_PER_SHUFFLE.get();
        int total = Math.max(1, HOLDERS.size());

        state.current.clear();
        for (List<ResourceLocation> ids : groups.values()) {
            List<ItemStack> pool = new ArrayList<>();
            List<ResourceLocation> free = new ArrayList<>();
            for (ResourceLocation id : ids) {
                pool.add(ORIGINALS.get(id));
                if (!state.pinned.containsKey(id)) free.add(id);
            }
            // a pinned recipe "owns" its result: take one copy of it out of the pool so every item stays obtainable
            for (ResourceLocation id : ids) {
                ItemStack locked = state.pinned.get(id);
                if (locked == null) continue;
                for (Iterator<ItemStack> it = pool.iterator(); it.hasNext(); ) {
                    if (ItemStack.isSameItemSameComponents(it.next(), locked)) {
                        it.remove();
                        break;
                    }
                }
            }
            // from time to time an UNCRAFTABLE item (nether star, blaze rod...) takes the place of a result
            if (!uncraftable.isEmpty() && !pool.isEmpty()) {
                int n = (int) Math.round((double) extras * ids.size() / total);
                for (int j = 0; j < n; j++) {
                    Item pick = uncraftable.get(random.nextInt(uncraftable.size()));
                    pool.set(random.nextInt(pool.size()), new ItemStack(pick));
                }
            }
            Collections.shuffle(pool, random);

            for (int i = 0; i < free.size(); i++) {
                ResourceLocation id = free.get(i);
                ItemStack original = ORIGINALS.get(id);
                ItemStack result = i < pool.size() ? pool.get(i).copy() : original.copy();
                if (Config.KEEP_ORIGINAL_COUNT.get()) {
                    result.setCount(Math.max(1, Math.min(original.getCount(), result.getMaxStackSize())));
                }
                state.current.put(id, result);
            }
        }

        state.dropSeed = random.nextLong();
        DropRandomizer.seed = state.dropSeed;
        state.ticksLeft = Config.intervalTicks();
        state.setDirty();
        applyAll(state);
        syncToPlayers(server);

        if (announce && Config.ANNOUNCE.get()) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.recipeamnesia.shuffled"), true);
        }
    }

    /** Amulet of Memory: adds time to the current iteration. Returns the seconds left. */
    public static int extendTimer(MinecraftServer server, int ticks) {
        ShuffleState state = ShuffleState.get(server);
        state.ticksLeft = Math.max(state.ticksLeft, 0) + ticks;
        state.setDirty();
        return state.ticksLeft / 20;
    }

    /** Writes pinned / shuffled / original results into the live recipe objects. */
    private static void applyAll(ShuffleState state) {
        for (Map.Entry<ResourceLocation, RecipeHolder<?>> e : HOLDERS.entrySet()) {
            ResourceLocation id = e.getKey();
            ItemStack stack = state.pinned.get(id);
            if (stack == null) stack = state.current.get(id);
            if (stack == null) stack = ORIGINALS.get(id);
            RecipeAccess.setResult(e.getValue().value(), stack.copy());
        }
    }

    /** Re-sends the recipe list to everybody and refreshes crafting grids that are open right now. */
    private static void syncToPlayers(MinecraftServer server) {
        List<RecipeHolder<?>> all = new ArrayList<>();
        for (RecipeHolder<?> holder : server.getRecipeManager().getOrderedRecipes()) all.add(holder);
        server.getPlayerList().broadcastAll(new ClientboundUpdateRecipesPacket(all));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // recompute the result slot of an open crafting table / 2x2 grid so a stale result can't be taken
            player.containerMenu.slotsChanged(player.getInventory());
            if (player.containerMenu != player.inventoryMenu) {
                player.inventoryMenu.slotsChanged(player.getInventory());
            }
        }
    }

    // ------------------------------------------------------------------ pins

    public static boolean isPinned(MinecraftServer server, ResourceLocation id) {
        return ShuffleState.get(server).pinned.containsKey(id);
    }

    /** Locks the recipe forever in its CURRENT form (the ingredients it has now -> the result it gives now). */
    public static PinResult pin(MinecraftServer server, ResourceLocation id) {
        ShuffleState state = ShuffleState.get(server);
        RecipeHolder<?> holder = HOLDERS.get(id);
        if (holder == null) return PinResult.UNKNOWN;
        if (state.pinned.containsKey(id)) return PinResult.ALREADY_PINNED;
        state.pinned.put(id, RecipeAccess.getResult(holder.value()).copy());
        state.current.remove(id);
        state.setDirty();
        return PinResult.OK;
    }

    /** Frees a pinned recipe. It keeps its current result until the next shuffle, then it is mixed again. */
    public static void unpin(MinecraftServer server, ResourceLocation id) {
        ShuffleState state = ShuffleState.get(server);
        ItemStack locked = state.pinned.remove(id);
        if (locked != null) {
            state.current.put(id, locked);
            state.setDirty();
        }
    }

    public static int unpinAll(MinecraftServer server) {
        ShuffleState state = ShuffleState.get(server);
        int n = state.pinned.size();
        for (Map.Entry<ResourceLocation, ItemStack> e : state.pinned.entrySet()) {
            state.current.put(e.getKey(), e.getValue());
        }
        state.pinned.clear();
        state.setDirty();
        return n;
    }

    private RecipeShuffler() {}
}
