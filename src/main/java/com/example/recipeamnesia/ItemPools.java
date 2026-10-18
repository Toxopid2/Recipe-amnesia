package com.example.recipeamnesia;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

/** Items that may appear as random results: every usable item, and the subset that no recipe can currently make. */
public final class ItemPools {
    /** Every item allowed to appear as a random mob drop / new recipe result. */
    public static volatile List<Item> usable = List.of();
    /** Usable items that are not the result of ANY recipe ("uncraftable"): they can occasionally get a recipe. */
    public static volatile List<Item> uncraftable = List.of();
    /** Every item that some mob can drop (built from the loot tables of all entity types + config extras). */
    public static volatile List<Item> mobDrops = List.of();

    /** Used only if scanning the loot tables found nothing (should not happen). */
    private static final String[] FALLBACK_MOB_DROPS = {
            "rotten_flesh", "bone", "string", "spider_eye", "gunpowder", "arrow", "feather", "leather", "beef", "porkchop",
            "chicken", "mutton", "rabbit_hide", "rabbit_foot", "ender_pearl", "blaze_rod", "ghast_tear", "slime_ball",
            "magma_cream", "ink_sac", "glow_ink_sac", "prismarine_shard", "prismarine_crystals", "phantom_membrane",
            "nether_star", "wither_skeleton_skull", "white_wool", "cod", "salmon", "iron_ingot", "carrot", "potato",
            "gold_nugget", "coal", "emerald", "totem_of_undying", "shulker_shell", "turtle_scute", "nautilus_shell"};

    public static void rebuild(MinecraftServer server, RecipeManager manager) {
        Set<Item> craftable = new HashSet<>();
        for (RecipeHolder<?> holder : manager.getOrderedRecipes()) {
            try {
                ItemStack result = holder.value().getResultItem(server.registryAccess());
                if (!result.isEmpty()) craftable.add(result.getItem());
            } catch (RuntimeException ignored) {
                // exotic modded recipe without a static result
            }
        }

        Set<String> blacklist = Config.itemPoolBlacklist();
        FeatureFlagSet flags = server.overworld().enabledFeatures();
        List<Item> all = new ArrayList<>();
        List<Item> unc = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || id.getNamespace().equals(RecipeAmnesia.MODID)) continue;
            if (item instanceof SpawnEggItem || !item.isEnabled(flags)) continue;
            if (blacklist.contains(id.toString())) continue;
            all.add(item);
            if (!craftable.contains(item)) unc.add(item);
        }
        Comparator<Item> byId = Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString());
        all.sort(byId);
        unc.sort(byId);
        usable = List.copyOf(all);
        uncraftable = List.copyOf(unc);
        mobDrops = buildMobDrops(server, new HashSet<>(all));
    }

    private static List<Item> buildMobDrops(MinecraftServer server, Set<Item> allowed) {
        Set<Item> found = new HashSet<>();
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "entities/" + id.getPath()));
            try {
                walk(server.reloadableRegistries().getLootTable(key), found, server, seen, 0);
            } catch (RuntimeException ignored) {
                // never let one odd loot table break the whole pool
            }
        }
        for (String extra : Config.extraMobDrops()) {
            ResourceLocation id = ResourceLocation.tryParse(extra);
            if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(found::add);
        }
        if (found.isEmpty()) {
            for (String name : FALLBACK_MOB_DROPS) {
                BuiltInRegistries.ITEM.getOptional(ResourceLocation.withDefaultNamespace(name)).ifPresent(found::add);
            }
        }
        List<Item> result = new ArrayList<>();
        for (Item item : found) {
            if (allowed.contains(item)) result.add(item); // respects itemPoolBlacklist, disabled items, our own items
        }
        result.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return List.copyOf(result);
    }

    /**
     * Structural walk over a loot table (pools -> entries -> nested/alternative entries) collecting every item entry.
     * Only fields of LootTable / LootPool / LootPoolEntryContainer are followed, so conditions and functions are skipped.
     */
    private static void walk(Object node, Set<Item> out, MinecraftServer server, Set<Object> seen, int depth) {
        if (node == null || depth > 16) return;
        if (node instanceof Either<?, ?> either) {
            either.left().ifPresent(o -> walk(o, out, server, seen, depth + 1));
            either.right().ifPresent(o -> walk(o, out, server, seen, depth + 1));
            return;
        }
        if (node instanceof ResourceKey<?> key) {
            if (key.registry().equals(Registries.LOOT_TABLE.location())) {
                @SuppressWarnings("unchecked")
                ResourceKey<LootTable> tableKey = (ResourceKey<LootTable>) key;
                walk(server.reloadableRegistries().getLootTable(tableKey), out, server, seen, depth + 1);
            }
            return;
        }
        if (node instanceof Iterable<?> iterable) {
            for (Object o : iterable) walk(o, out, server, seen, depth + 1);
            return;
        }
        if (!(node instanceof LootTable || node instanceof LootPool || node instanceof LootPoolEntryContainer)) return;
        if (!seen.add(node)) return;

        boolean isItemEntry = node instanceof LootItem;
        for (Class<?> c = node.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(node);
                    if (isItemEntry && value instanceof Holder<?> holder && holder.value() instanceof Item item) {
                        out.add(item);
                    } else if (isItemEntry && value instanceof Item item) {
                        out.add(item);
                    } else {
                        walk(value, out, server, seen, depth + 1);
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
    }

    private ItemPools() {}
}
