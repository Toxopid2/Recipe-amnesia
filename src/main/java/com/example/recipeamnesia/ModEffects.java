package com.example.recipeamnesia;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RecipeAmnesia.MODID);

    /** While active, the next recipe the player completes is locked forever. */
    public static final DeferredHolder<MobEffect, MobEffect> MEMORY =
            EFFECTS.register("memory", () -> new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xA64DFF));

    /** While active, completing a locked recipe un-locks it again. */
    public static final DeferredHolder<MobEffect, MobEffect> FORGETTING =
            EFFECTS.register("forgetting", () -> new MarkerEffect(MobEffectCategory.NEUTRAL, 0x8A9A78));

    /** An effect that does nothing by itself; the logic lives in {@link MemoryEffects}. */
    private static class MarkerEffect extends MobEffect {
        MarkerEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private ModEffects() {}
}
