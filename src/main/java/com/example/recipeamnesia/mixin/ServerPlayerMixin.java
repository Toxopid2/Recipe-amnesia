package com.example.recipeamnesia.mixin;

import java.util.List;

import com.example.recipeamnesia.MemoryEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla calls triggerRecipeCrafted every time a player takes the result of a recipe, no matter which station.
 * The full method descriptor + require = 0 means: if this method does not exist, nothing is injected and the game
 * still starts (the ItemCraftedEvent fallback then only covers the crafting grid).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "triggerRecipeCrafted(Lnet/minecraft/world/item/crafting/RecipeHolder;Ljava/util/List;)V",
            at = @At("HEAD"), require = 0)
    private void recipeamnesia$onRecipeCrafted(RecipeHolder<?> recipe, List<ItemStack> items, CallbackInfo ci) {
        MemoryEffects.onRecipeCompleted((ServerPlayer) (Object) this, recipe);
    }
}
