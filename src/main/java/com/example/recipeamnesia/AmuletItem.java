package com.example.recipeamnesia;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

/**
 * Amulet of Memory: adds time to the current shuffle iteration. Amulet of Amnesia: triggers the shuffle right now.
 * 4 uses, 10 s cooldown (configurable), cannot be enchanted or repaired.
 */
public class AmuletItem extends Item {
    private final boolean amnesia;

    public AmuletItem(Properties properties, boolean amnesia) {
        super(properties);
        this.amnesia = amnesia;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            MinecraftServer server = serverPlayer.getServer();
            if (amnesia) {
                RecipeShuffler.shuffle(server, true);
            } else {
                int seconds = RecipeShuffler.extendTimer(server, Config.AMULET_EXTEND_SECONDS.get() * 20);
                player.displayClientMessage(Component.translatable("message.recipeamnesia.extended",
                        Config.AMULET_EXTEND_SECONDS.get() / 60.0, seconds), true);
            }
            level.playSound(null, player.blockPosition(),
                    amnesia ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(this, Config.AMULET_COOLDOWN_SECONDS.get() * 20);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    // ---- no enchanting, no repairing
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
