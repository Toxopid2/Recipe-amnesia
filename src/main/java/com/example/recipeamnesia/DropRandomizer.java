package com.example.recipeamnesia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Randomized drops. Nothing is stored per block or mob: the replacement is a pure function of (seed, source, original item),
 * so it stays stable until the next shuffle changes the seed.
 * <ul>
 *   <li>block categories (earth, stone, logs): another BLOCK of the same category;</li>
 *   <li>ore categories: what one of the ores drops when mined (raw iron, redstone, ancient debris itself...);</li>
 *   <li>mobs: something that SOME mob drops.</li>
 * </ul>
 */
public final class DropRandomizer {
    /** Set by {@link RecipeShuffler} on load and on every shuffle. */
    public static volatile long seed = 0L;

    private static final Map<String, List<Item>> CATEGORY_MEMBERS = new HashMap<>();
    private static Set<String> exclusions;

    /** Called when tags/config may have changed (server start, /reload). */
    public static synchronized void invalidate() {
        CATEGORY_MEMBERS.clear();
        exclusions = null;
    }

    // ------------------------------------------------------------------ blocks

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!Config.RANDOMIZE_BLOCK_DROPS.get() || event.getDrops().isEmpty()) return;
        BlockState state = event.getState();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (isExcluded(blockId)) return;

        // ores first: they give what an ore gives when mined, not a block
        for (String tagName : Config.oreDropTags()) {
            TagKey<Block> tag = blockTag(tagName);
            if (tag == null || !state.is(tag)) continue;
            List<Item> pool = members("ore:" + tagName, tag, true, event.getLevel(), event.getPos());
            if (!pool.isEmpty()) {
                replaceAll(event, pool, blockId + "|" + tagName);
                return;
            }
        }
        for (String tagName : Config.dropCategoryTags()) {
            TagKey<Block> tag = blockTag(tagName);
            if (tag == null || !state.is(tag)) continue;
            List<Item> pool = members("block:" + tagName, tag, false, event.getLevel(), event.getPos());
            if (!pool.isEmpty()) {
                replaceAll(event, pool, blockId + "|" + tagName);
                return; // first matching category wins
            }
        }
    }

    private static void replaceAll(BlockDropsEvent event, List<Item> pool, String source) {
        for (ItemEntity drop : event.getDrops()) {
            ItemStack original = drop.getItem();
            if (original.isEmpty() || isExcluded(BuiltInRegistries.ITEM.getKey(original.getItem()))) continue;
            drop.setItem(replacement(original, pool, source));
        }
    }

    private static TagKey<Block> blockTag(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        return id == null ? null : TagKey.create(Registries.BLOCK, id);
    }

    /**
     * Pool of a category. For ore categories ({@code mined == true}) the items an ore block drops when broken without any tool
     * (silk touch is not applied, so you get raw iron, redstone, ancient debris itself...). If an ore yields nothing, its block item is used.
     */
    private static synchronized List<Item> members(String cacheKey, TagKey<Block> tag, boolean mined, ServerLevel level, BlockPos pos) {
        List<Item> cached = CATEGORY_MEMBERS.get(cacheKey);
        if (cached != null) return cached;

        Set<Item> items = new LinkedHashSet<>();
        Optional<HolderSet.Named<Block>> named = BuiltInRegistries.BLOCK.getTag(tag);
        if (named.isPresent()) {
            for (var holder : named.get()) {
                Block block = holder.value();
                if (isExcluded(BuiltInRegistries.BLOCK.getKey(block))) continue;
                boolean added = false;
                if (mined) {
                    try {
                        for (ItemStack drop : Block.getDrops(block.defaultBlockState(), level, pos, null)) {
                            if (!drop.isEmpty() && !isExcluded(BuiltInRegistries.ITEM.getKey(drop.getItem()))) {
                                items.add(drop.getItem());
                                added = true;
                            }
                        }
                    } catch (RuntimeException ignored) {
                        // fall through to the block item
                    }
                }
                if (!added) {
                    Item item = block.asItem();
                    if (item != Items.AIR && !isExcluded(BuiltInRegistries.ITEM.getKey(item))) items.add(item);
                }
            }
        }
        List<Item> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        CATEGORY_MEMBERS.put(cacheKey, sorted);
        return sorted;
    }

    // ------------------------------------------------------------------ mobs

    public static void onLivingDrops(LivingDropsEvent event) {
        if (!Config.RANDOMIZE_MOB_DROPS.get() || event.getDrops().isEmpty()) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return; // never touch what a dead player drops
        List<Item> pool = ItemPools.mobDrops; // only things some mob really drops
        if (pool.isEmpty()) return;

        String source = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack original = drop.getItem();
            if (original.isEmpty() || isExcluded(BuiltInRegistries.ITEM.getKey(original.getItem()))) continue;
            drop.setItem(replacement(original, pool, source));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static ItemStack replacement(ItemStack original, List<Item> pool, String source) {
        ResourceLocation originalId = BuiltInRegistries.ITEM.getKey(original.getItem());
        long hash = seed ^ ((source + "#" + originalId).hashCode() * 0x9E3779B97F4A7C15L);
        Item item = pool.get(new Random(hash).nextInt(pool.size()));
        return new ItemStack(item, Math.max(1, Math.min(original.getCount(), item.getDefaultMaxStackSize())));
    }

    private static synchronized boolean isExcluded(ResourceLocation id) {
        if (id.getNamespace().equals(RecipeAmnesia.MODID)) return true; // memoryum is never touched
        if (exclusions == null) exclusions = Config.dropExclusions();
        return exclusions.contains(id.toString());
    }

    private DropRandomizer() {}
}
