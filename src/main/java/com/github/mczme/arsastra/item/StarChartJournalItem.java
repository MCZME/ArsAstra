package com.github.mczme.arsastra.item;

import com.github.mczme.arsastra.client.renderer.StarChartJournalItemRenderer;
import com.github.mczme.arsastra.registry.AAComponents;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.constant.DataTickets;

public class StarChartJournalItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenPlay("book.open");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("book.idle");

    public StarChartJournalItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.github.mczme.arsastra.client.gui.StarChartJournalScreen());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // 只在客户端处理动画状态标记
        if (level.isClientSide && entity instanceof Player player) {
            boolean isHeld = isSelected || player.getOffhandItem() == stack;
            // 使用 Data Component 获取状态
            boolean wasOpen = stack.getOrDefault(AAComponents.IS_OPEN, false);
            
            if (isHeld && !wasOpen) {
                // 刚拿出来：标记为 Open
                stack.set(AAComponents.IS_OPEN, true);
            } else if (!isHeld && wasOpen) {
                // 刚收起来：标记为 Closed
                stack.set(AAComponents.IS_OPEN, false);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            ItemDisplayContext context = event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            ItemStack stack = event.getData(DataTickets.ITEMSTACK);

            if (context != null) {  
                if (stack != null && stack.getOrDefault(AAComponents.IS_OPEN, false)) {
                    return event.setAndContinue(OPEN_ANIM);
                }
            }

            return event.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private StarChartJournalItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new StarChartJournalItemRenderer();
                }
                return this.renderer;
            }
        });
    }
}
