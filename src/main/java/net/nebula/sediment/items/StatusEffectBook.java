package net.nebula.sediment.items;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.nebula.sediment.common.StatusEffectDiscovery;
import net.nebula.sediment.common.StatusEffectGameRules;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class StatusEffectBook extends Item {

    public StatusEffectBook() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        boolean enabled = level.getGameRules().getBoolean(StatusEffectGameRules.STATUS_EFFECT_BOOK_ENABLED);
        if (!enabled) {
            serverPlayer.displayClientMessage(Component.translatable("item.sediment_lib.status_effect_book.disabled"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (StatusEffectDiscovery.isBookUnlocked(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("item.sediment_lib.status_effect_book.already_unlocked"), true);
            return InteractionResultHolder.fail(stack);
        }

        StatusEffectDiscovery.unlockBook(serverPlayer);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverPlayer.displayClientMessage(Component.translatable("item.sediment_lib.status_effect_book.unlocked"), true);

        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.sediment_lib.status_effect_book.desc"));
    }
}