package com.github.mczme.arsastra.block;

import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.github.mczme.arsastra.registry.AABlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CopperTunBlock extends BaseEntityBlock {
    public static final MapCodec<CopperTunBlock> CODEC = simpleCodec(CopperTunBlock::new);
    
    private static final VoxelShape SHAPE = Shapes.or(
        box(2, 0, 2, 14, 1, 14),   // Base (Cube 1)
        box(2, 9, 2, 14, 10, 14),  // Inner Plate (Cube 2)
        box(1, 1, 1, 15, 10, 2),   // Lower Wall N (Cube 3)
        box(1, 1, 14, 15, 10, 15), // Lower Wall S (Cube 4)
        box(14, 1, 2, 15, 10, 14), // Lower Wall E (Cube 5)
        box(1, 1, 2, 2, 10, 14),   // Lower Wall W (Cube 6)
        box(2, 10, 2, 14, 12, 3),  // Neck N (Cube 7)
        box(2, 10, 13, 14, 12, 14),// Neck S (Cube 10)
        box(13, 10, 3, 14, 12, 13),// Neck E (Cube 12)
        box(2, 10, 3, 3, 12, 13),  // Neck W (Cube 11)
        box(2, 12, 1, 14, 15, 2),  // Rim N (Cube 8)
        box(2, 12, 14, 14, 15, 15),// Rim S (Cube 9)
        box(14, 12, 2, 15, 15, 14),// Rim E (Cube 13)
        box(1, 12, 2, 2, 15, 14)   // Rim W (Cube 14)
    );

    public CopperTunBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CopperTunBlockEntity tun) {
            InteractionResult result = tun.onUse(player, hand);
            if (result == InteractionResult.SUCCESS) {
                return ItemInteractionResult.SUCCESS;
            }
            if (result == InteractionResult.CONSUME) {
                return ItemInteractionResult.CONSUME;
            }
            if (result == InteractionResult.FAIL) {
                return ItemInteractionResult.FAIL;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperTunBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // 根据端侧返回不同的 ticker
        if (level.isClientSide) {
            return createTickerHelper(blockEntityType, AABlockEntities.COPPER_TUN.get(), CopperTunBlockEntity::clientTick);
        } else {
            return createTickerHelper(blockEntityType, AABlockEntities.COPPER_TUN.get(), CopperTunBlockEntity::serverTick);
        }
    }
}
