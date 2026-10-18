package com.example.recipeamnesia;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Adds min..max Memoryum Blocks with the given chance to the loot tables selected by the modifier's conditions. */
public class AddMemoryumModifier extends LootModifier {
    public static final Supplier<MapCodec<AddMemoryumModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(inst.group(
                    Codec.INT.fieldOf("min").forGetter(m -> m.min),
                    Codec.INT.fieldOf("max").forGetter(m -> m.max),
                    Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance)
            )).apply(inst, AddMemoryumModifier::new)));

    private final int min;
    private final int max;
    private final float chance;

    public AddMemoryumModifier(LootItemCondition[] conditions, int min, int max, float chance) {
        super(conditions);
        this.min = min;
        this.max = max;
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() >= chance) return loot;
        int count = min + (max > min ? context.getRandom().nextInt(max - min + 1) : 0);
        if (count > 0) loot.add(new ItemStack(ModItems.MEMORYUM_BLOCK.get(), count));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
