package com.example.recipeamnesia;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Bottle of Memory ({@code forgetting == false}) gives the MEMORY effect, Bottle of Forgetting gives FORGETTING.
 * The two effects exclude each other. What they do is implemented in {@link MemoryEffects}.
 */
public class MemoryBottleItem extends Item {
    private final boolean forgetting;

    public MemoryBottleItem(Properties properties, boolean forgetting) {
        super(properties);
        this.forgetting = forgetting;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            Holder<MobEffect> give = forgetting ? ModEffects.FORGETTING : ModEffects.MEMORY;
            Holder<MobEffect> take = forgetting ? ModEffects.MEMORY : ModEffects.FORGETTING;
            player.removeEffect(take);
            player.addEffect(new MobEffectInstance(give, Config.EFFECT_SECONDS.get() * 20, 0, false, true, true));
            player.displayClientMessage(Component.translatable(
                    forgetting ? "message.recipeamnesia.forgetting_started" : "message.recipeamnesia.memory_started"), true);
        }

        if (entity instanceof Player player) {
            if (player.getAbilities().instabuild) return stack;
            stack.shrink(1);
            ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) return empty;
            if (!player.getInventory().add(empty)) player.drop(empty, false);
        }
        return stack;
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
