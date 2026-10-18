package com.example.recipeamnesia;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;

/**
 * Reads / replaces the (private final) result stack of the vanilla recipe classes via reflection.
 * The field is looked up by TYPE (the one non-static ItemStack field), not by name, so it does not depend on mappings.
 * Recipes are mutated in place; the server then re-sends the recipe list to every client.
 */
public final class RecipeAccess {
    /** Recipe classes whose result is a plain ItemStack field. Special/custom recipes (dyeing, fireworks, maps...) are ignored. */
    private static final List<Class<?>> BASES = List.of(
            ShapedRecipe.class, ShapelessRecipe.class, AbstractCookingRecipe.class,
            SingleItemRecipe.class, SmithingTransformRecipe.class);

    private static final Map<Class<?>, Field> RESULT_FIELDS = new HashMap<>();

    public static Class<?> baseOf(Recipe<?> recipe) {
        for (Class<?> base : BASES) {
            if (base.isInstance(recipe)) return base;
        }
        return null;
    }

    public static boolean isSupported(Recipe<?> recipe) {
        return baseOf(recipe) != null;
    }

    private static synchronized Field resultField(Class<?> base) {
        Field cached = RESULT_FIELDS.get(base);
        if (cached != null) return cached;
        for (Class<?> c = base; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) && f.getType() == ItemStack.class) {
                    f.setAccessible(true);
                    RESULT_FIELDS.put(base, f);
                    return f;
                }
            }
        }
        return null;
    }

    public static ItemStack getResult(Recipe<?> recipe) {
        Class<?> base = baseOf(recipe);
        Field f = base == null ? null : resultField(base);
        if (f == null) return ItemStack.EMPTY;
        try {
            return (ItemStack) f.get(recipe);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean setResult(Recipe<?> recipe, ItemStack stack) {
        Class<?> base = baseOf(recipe);
        Field f = base == null ? null : resultField(base);
        if (f == null) return false;
        try {
            f.set(recipe, stack);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * A recipe is shuffled only if it is a supported type, allowed by the blacklists (exceptions win), and the result
     * field really is what getResultItem returns. Recipes of this mod are never shuffled.
     */
    public static boolean isShufflable(RecipeHolder<?> holder, HolderLookup.Provider registries, Config.RecipeFilters filters) {
        Recipe<?> recipe = holder.value();
        if (!isSupported(recipe)) return false;
        ResourceLocation id = holder.id();
        if (id.getNamespace().equals(RecipeAmnesia.MODID)) return false;

        if (!filters.exceptions().contains(id.toString())) {
            if (filters.blacklist().contains(id.toString()) || filters.blacklist().contains(id.getNamespace())) return false;
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            if (type != null && filters.categories().contains(type.toString())) return false;
        }

        ItemStack field = getResult(recipe);
        if (field.isEmpty()) return false;
        return ItemStack.matches(field, recipe.getResultItem(registries));
    }

    /** Recipe category (recipe type id) used for grouping. */
    public static String categoryOf(Recipe<?> recipe) {
        ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        return type == null ? "unknown" : type.toString();
    }

    private RecipeAccess() {}
}
