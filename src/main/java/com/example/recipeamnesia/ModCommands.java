package com.example.recipeamnesia;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ModCommands {
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("recipeamnesia")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("shuffle").executes(ctx -> {
                    RecipeShuffler.shuffle(ctx.getSource().getServer(), true);
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.recipeamnesia.shuffled"), true);
                    return 1;
                }))
                .then(Commands.literal("unpin_all").executes(ctx -> {
                    int n = RecipeShuffler.unpinAll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.recipeamnesia.unpinned", n), true);
                    return n;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ShuffleState st = ShuffleState.get(ctx.getSource().getServer());
                    int seconds = Math.max(0, st.ticksLeft) / 20;
                    ctx.getSource().sendSuccess(() -> Component.translatable("command.recipeamnesia.status",
                            RecipeShuffler.shufflableCount(), st.pinned.size(), seconds), false);
                    return 1;
                })));
    }

    private ModCommands() {}
}
