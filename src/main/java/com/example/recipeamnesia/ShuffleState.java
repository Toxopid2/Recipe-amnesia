package com.example.recipeamnesia;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world data: the current (shuffled) result of every recipe, the pinned recipes and the shuffle timer. */
public class ShuffleState extends SavedData {
    private static final String NAME = RecipeAmnesia.MODID + "_state";

    /** recipe id -> result it currently gives (only for shuffled, non-pinned recipes). */
    public final Map<ResourceLocation, ItemStack> current = new LinkedHashMap<>();
    /** recipe id -> result locked forever by a Bottle of Memory. */
    public final Map<ResourceLocation, ItemStack> pinned = new LinkedHashMap<>();
    public int ticksLeft = -1;
    /** Seed of the current drop randomization; changes with every shuffle. */
    public long dropSeed = 0L;

    public static ShuffleState get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(ShuffleState::new, ShuffleState::load), NAME);
    }

    public static ShuffleState load(CompoundTag tag, HolderLookup.Provider registries) {
        ShuffleState state = new ShuffleState();
        state.ticksLeft = tag.getInt("TicksLeft");
        state.dropSeed = tag.getLong("DropSeed");
        readMap(tag.getList("Current", Tag.TAG_COMPOUND), state.current, registries);
        readMap(tag.getList("Pinned", Tag.TAG_COMPOUND), state.pinned, registries);
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("TicksLeft", ticksLeft);
        tag.putLong("DropSeed", dropSeed);
        tag.put("Current", writeMap(current, registries));
        tag.put("Pinned", writeMap(pinned, registries));
        return tag;
    }

    private static ListTag writeMap(Map<ResourceLocation, ItemStack> map, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceLocation, ItemStack> e : map.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putString("Recipe", e.getKey().toString());
            entry.put("Result", e.getValue().save(registries));
            list.add(entry);
        }
        return list;
    }

    private static void readMap(ListTag list, Map<ResourceLocation, ItemStack> into, HolderLookup.Provider registries) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Recipe"));
            if (id == null || !entry.contains("Result")) continue;
            ItemStack stack = ItemStack.parse(registries, entry.get("Result")).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) into.put(id, stack);
        }
    }
}
