package com.example.recipeamnesia;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RecipeAmnesia.MODID);

    /** Generates naturally deep underground (see data/recipeamnesia/worldgen), drops itself, needs a diamond pickaxe. */
    public static final DeferredBlock<Block> MEMORYUM_BLOCK = BLOCKS.registerSimpleBlock("memoryum_block",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(10.0F, 100.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 4));

    private ModBlocks() {}
}
