package com.example.recipeamnesia;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RecipeAmnesia.MODID);

    public static final DeferredItem<BlockItem> MEMORYUM_BLOCK = ITEMS.registerSimpleBlockItem("memoryum_block", ModBlocks.MEMORYUM_BLOCK);

    /** Smelt a Memoryum Block in a furnace. */
    public static final DeferredItem<Item> TRUE_MEMORYUM_SHARD = ITEMS.registerSimpleItem("true_memoryum_shard",
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<MemoryBottleItem> MEMORY_BOTTLE = ITEMS.registerItem("memory_bottle",
            props -> new MemoryBottleItem(props, false), new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static final DeferredItem<MemoryBottleItem> FORGETTING_BOTTLE = ITEMS.registerItem("forgetting_bottle",
            props -> new MemoryBottleItem(props, true), new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static final DeferredItem<AmuletItem> MEMORY_AMULET = ITEMS.registerItem("memory_amulet",
            props -> new AmuletItem(props, false), new Item.Properties().durability(4).rarity(Rarity.EPIC));

    public static final DeferredItem<AmuletItem> AMNESIA_AMULET = ITEMS.registerItem("amnesia_amulet",
            props -> new AmuletItem(props, true), new Item.Properties().durability(4).rarity(Rarity.EPIC));

    private ModItems() {}
}
