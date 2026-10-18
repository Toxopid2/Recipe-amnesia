package com.example.recipeamnesia;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(RecipeAmnesia.MODID)
public class RecipeAmnesia {
    public static final String MODID = "recipeamnesia";

    public RecipeAmnesia(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModLootModifiers.SERIALIZERS.register(modBus);
        modBus.addListener(this::addCreative);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(RecipeShuffler::onServerTick);
        NeoForge.EVENT_BUS.addListener(ModCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(MemoryEffects::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(MemoryEffects::onLogout);
        NeoForge.EVENT_BUS.addListener(DropRandomizer::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(DropRandomizer::onLivingDrops);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.MEMORYUM_BLOCK.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.TRUE_MEMORYUM_SHARD.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.MEMORY_BOTTLE.get());
            event.accept(ModItems.FORGETTING_BOTTLE.get());
            event.accept(ModItems.MEMORY_AMULET.get());
            event.accept(ModItems.AMNESIA_AMULET.get());
        }
    }
}
