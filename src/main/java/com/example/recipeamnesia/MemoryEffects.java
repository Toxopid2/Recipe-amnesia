package com.example.recipeamnesia;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.RecipeCraftingHolder;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The gameplay of the two effects. {@link #onRecipeCompleted} is called whenever a player takes the result of ANY
 * recipe (crafting grid, furnace, blast furnace, smoker, stonecutter, smithing table) - from the mixin into
 * ServerPlayer.triggerRecipeCrafted, and additionally from NeoForge's ItemCraftedEvent as a fallback for the crafting grid.
 */
public final class MemoryEffects {
    /** player -> "tick|recipe", so the mixin and the event don't handle the same craft twice. */
    private static final Map<UUID, String> LAST = new HashMap<>();

    public static void onRecipeCompleted(ServerPlayer player, RecipeHolder<?> holder) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        boolean memory = player.hasEffect(ModEffects.MEMORY);
        boolean forgetting = player.hasEffect(ModEffects.FORGETTING);
        if (!memory && !forgetting) return;

        ResourceLocation id = holder.id();
        String key = server.getTickCount() + "|" + id;
        if (key.equals(LAST.put(player.getUUID(), key))) return;

        ItemStack result = RecipeAccess.getResult(holder.value());

        if (forgetting && RecipeShuffler.isPinned(server, id)) {
            RecipeShuffler.unpin(server, id);
            player.removeEffect(ModEffects.FORGETTING);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.recipeamnesia.forgotten", result.getHoverName()), false);
            return;
        }

        if (memory) {
            switch (RecipeShuffler.pin(server, id)) {
                case OK -> {
                    player.removeEffect(ModEffects.MEMORY);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.displayClientMessage(Component.translatable("message.recipeamnesia.remembered", result.getHoverName()), false);
                }
                case ALREADY_PINNED -> player.displayClientMessage(Component.translatable("message.recipeamnesia.already_pinned"), true);
                case UNKNOWN -> player.displayClientMessage(Component.translatable("message.recipeamnesia.cannot_remember"), true);
            }
        }
    }

    /** Fallback for the crafting grid: the result slot's container knows which recipe was used. */
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.hasEffect(ModEffects.MEMORY) && !player.hasEffect(ModEffects.FORGETTING)) return;
        for (Slot slot : player.containerMenu.slots) {
            if (slot instanceof ResultSlot && slot.container instanceof RecipeCraftingHolder holder) {
                RecipeHolder<?> used = holder.getRecipeUsed();
                if (used != null) onRecipeCompleted(player, used);
                return;
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
    }

    private MemoryEffects() {}
}
