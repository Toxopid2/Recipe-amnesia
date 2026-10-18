package com.example.recipeamnesia;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------ shuffling
    public static final ModConfigSpec.IntValue INTERVAL_SECONDS = B
            .comment("How often (in seconds) everything is reshuffled (\"amnesia\"). 300 = 5 minutes.")
            .defineInRange("intervalSeconds", 300, 10, 86400);

    public static final ModConfigSpec.BooleanValue SHUFFLE_WITHIN_TYPE = B
            .comment("false (default): NO restrictions - a pickaxe can end up in a crafting table, a furnace, a stonecutter,",
                    "a smithing table... true: recipes are only mixed between recipes of the same station.")
            .define("shuffleWithinRecipeType", false);

    public static final ModConfigSpec.BooleanValue KEEP_ORIGINAL_COUNT = B
            .comment("true: the amount an item is crafted in stays the same (4 planks stay 4), only the ingredients change.")
            .define("keepOriginalCount", false);

    public static final ModConfigSpec.BooleanValue ANNOUNCE = B
            .comment("Show an action-bar message 10 seconds before and right after every shuffle.")
            .define("announceShuffle", true);

    public static final ModConfigSpec.IntValue UNCRAFTABLE_RECIPES_PER_SHUFFLE = B
            .comment("Every shuffle this many recipes get a random UNCRAFTABLE item (nether star, blaze rod, ...) as their result.",
                    "0 disables it.")
            .defineInRange("uncraftableRecipesPerShuffle", 3, 0, 1000);

    // ------------------------------------------------------------------ recipe filters
    public static final ModConfigSpec.ConfigValue<List<? extends String>> RECIPE_BLACKLIST = B
            .comment("Recipes that are NOT shuffled. Entries: a full recipe id (\"minecraft:glass_bottle\") or a whole mod id (\"create\").",
                    "The glass bottle is listed because the Bottles of Memory/Forgetting are crafted with one.")
            .defineListAllowEmpty("recipeBlacklist", List.of("minecraft:glass_bottle"), () -> "", Config::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> RECIPE_CATEGORY_BLACKLIST = B
            .comment("Recipe categories (recipe types) in which NOTHING is shuffled, e.g. \"minecraft:smelting\",",
                    "\"minecraft:crafting\", \"minecraft:blasting\", \"minecraft:smoking\", \"minecraft:campfire_cooking\",",
                    "\"minecraft:stonecutting\", \"minecraft:smithing\".")
            .defineListAllowEmpty("recipeCategoryBlacklist", List.of(), () -> "", Config::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST_EXCEPTIONS = B
            .comment("Exceptions: recipe ids listed here are shuffled ANYWAY, even if their mod, their category",
                    "or the recipe itself is blacklisted above.")
            .defineListAllowEmpty("blacklistExceptions", List.of(), () -> "", Config::isString);

    // ------------------------------------------------------------------ item pool
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_POOL_BLACKLIST = B
            .comment("Items that can NEVER appear as a random result: neither as a new recipe result nor as a random mob drop.",
                    "(Spawn eggs and items of this mod are always excluded.)")
            .defineListAllowEmpty("itemPoolBlacklist", List.of(
                    "minecraft:bedrock", "minecraft:barrier", "minecraft:light", "minecraft:structure_void",
                    "minecraft:structure_block", "minecraft:jigsaw", "minecraft:command_block",
                    "minecraft:chain_command_block", "minecraft:repeating_command_block",
                    "minecraft:command_block_minecart", "minecraft:debug_stick", "minecraft:knowledge_book",
                    "minecraft:spawner", "minecraft:trial_spawner", "minecraft:vault", "minecraft:end_portal_frame",
                    "minecraft:reinforced_deepslate", "minecraft:budding_amethyst", "minecraft:petrified_oak_slab",
                    "minecraft:farmland", "minecraft:dirt_path", "minecraft:chorus_plant", "minecraft:frogspawn",
                    "minecraft:enchanted_book", "minecraft:written_book"), () -> "", Config::isString);

    // ------------------------------------------------------------------ drops
    public static final ModConfigSpec.BooleanValue RANDOMIZE_MOB_DROPS = B
            .comment("Mob drops are randomized: a mob drops something that SOME mob drops (a sheep may drop rotten flesh, a blaze rod,",
                    "a nether star...). Changes with every shuffle.")
            .define("randomizeMobDrops", true);

    public static final ModConfigSpec.BooleanValue RANDOMIZE_BLOCK_DROPS = B
            .comment("Breaking a block of a category below gives another block of the SAME category. Changes with every shuffle.")
            .define("randomizeBlockDrops", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DROP_CATEGORY_TAGS = B
            .comment("Block tags of BLOCK drop categories (defaults: earth, stone, logs).",
                    "Breaking a block of such a category gives another BLOCK of the same category.")
            .defineListAllowEmpty("dropCategoryTags", List.of(
                    "minecraft:dirt", "minecraft:base_stone_overworld", "minecraft:logs"), () -> "", Config::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ORE_DROP_TAGS = B
            .comment("Block tags of ORE drop categories (default: all ores).",
                    "Breaking an ore gives what one of the ores gives when mined (raw iron, raw gold, redstone, diamond, coal...),",
                    "NOT the ore block. If the rolled ore drops itself (ancient debris), you get that.")
            .defineListAllowEmpty("oreDropTags", List.of("c:ores"), () -> "", Config::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_MOB_DROPS = B
            .comment("Items that mobs drop WITHOUT a loot table (so they can't be detected automatically) and should be part of the",
                    "random mob-drop pool. The pool itself is built from the loot tables of all mobs.")
            .defineListAllowEmpty("extraMobDrops", List.of("minecraft:nether_star", "minecraft:dragon_egg"), () -> "", Config::isString);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DROP_EXCLUSIONS = B
            .comment("Blocks/items that never take part in drop randomization (neither replaced nor given out).",
                    "Memoryum is not an ore, but it is listed here (and hard-coded) for safety.")
            .defineListAllowEmpty("dropExclusions", List.of("recipeamnesia:memoryum_block"), () -> "", Config::isString);

    // ------------------------------------------------------------------ bottles and amulets
    public static final ModConfigSpec.IntValue EFFECT_SECONDS = B
            .comment("How long the Memory / Forgetting effect lasts after drinking a bottle (seconds).")
            .defineInRange("effectSeconds", 600, 10, 86400);

    public static final ModConfigSpec.IntValue AMULET_EXTEND_SECONDS = B
            .comment("Amulet of Memory: how many seconds it adds to the current shuffle iteration. 300 = 5 minutes.")
            .defineInRange("amuletExtendSeconds", 300, 1, 86400);

    public static final ModConfigSpec.IntValue AMULET_COOLDOWN_SECONDS = B
            .comment("Cooldown of both amulets (seconds).")
            .defineInRange("amuletCooldownSeconds", 10, 0, 3600);

    static final ModConfigSpec SPEC = B.build();

    // ------------------------------------------------------------------ helpers
    public record RecipeFilters(Set<String> blacklist, Set<String> categories, Set<String> exceptions) {}

    public static RecipeFilters recipeFilters() {
        return new RecipeFilters(set(RECIPE_BLACKLIST), set(RECIPE_CATEGORY_BLACKLIST), set(BLACKLIST_EXCEPTIONS));
    }

    public static int intervalTicks() {
        return INTERVAL_SECONDS.get() * 20;
    }

    public static Set<String> itemPoolBlacklist() {
        return set(ITEM_POOL_BLACKLIST);
    }

    public static Set<String> dropExclusions() {
        return set(DROP_EXCLUSIONS);
    }

    public static List<String> dropCategoryTags() {
        return list(DROP_CATEGORY_TAGS);
    }

    public static List<String> oreDropTags() {
        return list(ORE_DROP_TAGS);
    }

    public static List<String> extraMobDrops() {
        return list(EXTRA_MOB_DROPS);
    }

    private static List<String> list(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return value.get().stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private static Set<String> set(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return value.get().stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
    }

    private static boolean isString(Object o) {
        return o instanceof String;
    }

    private Config() {}
}
