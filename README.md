# Recipe Amnesia (NeoForge 1.21.1)

Every 5 minutes ("amnesia") EVERYTHING is reshuffled: recipes, mob drops, block drops.
Bottles let you lock recipes forever, amulets bend the timer.

## Build
JDK 21 required. `./gradlew build` -> `build/libs/recipeamnesia-1.0.0.jar`; `./gradlew runClient` to test.
The jar goes into `mods/` on the server AND on every client.

## Content
| item | how to get / craft | what it does |
|---|---|---|
| Memoryum Block | generated in the overworld, y -64..0 (like deepslate diamonds), a bit more common than ancient debris; diamond pickaxe | crafting ingredient |
| Shard of True Memoryum | smelt a Memoryum Block in a furnace | amulet ingredient |
| Bottle of Memory | 8 Memoryum Blocks around a glass bottle | drink -> effect **Memory**: the first recipe you complete is locked forever |
| Bottle of Forgetting | glass bottle + Memoryum Block + rotten flesh (shapeless) | drink -> effect **Forgetting**: completing a locked recipe unlocks it |
| Amulet of Memory | shard in the middle, 8 nuggets (`#c:nuggets`) around | right-click: +5 min to the current iteration. 4 uses, 10 s cooldown |
| Amulet of Amnesia | same, but the bottom-middle nugget is a Bottle of Forgetting | right-click: reshuffle RIGHT NOW. 4 uses, 10 s cooldown |

Amulets cannot be enchanted (no table, no anvil book, no enchant compat) and cannot be repaired.
Recipes of this mod are never shuffled.

## What gets shuffled
* **Recipes** - results are dealt out between recipes, i.e. an item is made from different ingredients each time.
  By default with NO restrictions (a pickaxe may be a crafting-table, furnace, stonecutter or smithing recipe);
  set `shuffleWithinRecipeType = true` to keep items at their station.
* **Uncraftable items** (nether star, blaze rod...) occasionally get a recipe: every shuffle `uncraftableRecipesPerShuffle`
  (default 3) results are replaced by random uncraftable items.
* **Mob drops** - every drop of a mob is replaced by a random drop OF A MOB: the pool is built from the loot tables of all mobs
  (+ `extraMobDrops`), so a sheep may drop rotten flesh, a blaze rod or a nether star. Players' death drops are never touched.
* **Block drops by category** (block tags): earth `#minecraft:dirt`, stone `#minecraft:base_stone_overworld`,
  logs `#minecraft:logs`. Breaking a block of a category gives another BLOCK of the same category.
* **Ores** (`oreDropTags`, default `#c:ores`): you get what one of the ores gives when MINED (raw iron, raw gold, redstone, diamond, coal...),
  not an ore block; if the rolled ore drops itself (ancient debris) you get ancient debris.
  Memoryum is NOT an ore and is additionally excluded in code and in `dropExclusions`.
* Drop randomization is a pure function of a seed that changes with every shuffle, so e.g. dirt gives the same
  replacement until the next amnesia.

## Memoryum in structure chests
End city 2-7 (100%) · desert pyramid / jungle temple 1 (70%) · nether fortress 1-2 (80%) ·
bastion chests 1-3 (30%), bastion treasure room 1-5 (100%) · stronghold chests 1-2 (50%).
Edit `data/recipeamnesia/loot_modifiers/*.json` (min / max / chance).

## Config (`config/recipeamnesia-common.toml`)
intervalSeconds · shuffleWithinRecipeType · keepOriginalCount · announceShuffle · uncraftableRecipesPerShuffle ·
**recipeBlacklist** (recipe ids or mod ids) · **recipeCategoryBlacklist** (recipe types, e.g. `minecraft:smelting`) ·
**blacklistExceptions** (shuffled anyway) · itemPoolBlacklist · randomizeMobDrops · randomizeBlockDrops ·
**dropCategoryTags** · **oreDropTags** · extraMobDrops · dropExclusions · effectSeconds · amuletExtendSeconds · amuletCooldownSeconds

NOTE: the old `excludedNamespaces` / `excludedRecipes` options were replaced by `recipeBlacklist` - delete an old config file.

## Commands (op level 2)
`/recipeamnesia shuffle` · `/recipeamnesia status` · `/recipeamnesia unpin_all`

## Notes
* "Recipe completed" detection: a Mixin into `ServerPlayer.triggerRecipeCrafted` (all stations) + NeoForge's ItemCraftedEvent
  as a fallback for the crafting grid. The mixin has `require = 0`; if it can't apply, only the crafting grid supports Memory/Forgetting.
* Special recipes without a fixed result (armor dyeing, fireworks, maps, trims) are not shuffled.
* JEI/EMI re-index recipes after each shuffle; on big modpacks this can hitch for a moment.

## License
MIT, Copyright (c) 2026 tohioze.
