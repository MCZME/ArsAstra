package com.github.mczme.arsastra.block;

import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CopperTunBlock extends BaseEntityBlock {
    public static final MapCodec<CopperTunBlock> CODEC = simpleCodec(CopperTunBlock::new);
    // 简单的容器形状: 底部 + 四壁
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), // Base
            Block.box(0, 0, 0, 2, 16, 16), // Wall
            Block.box(14, 0, 0, 16, 16, 16), // Wall
            Block.box(2, 0, 0, 14, 16, 2), // Wall
            Block.box(2, 0, 14, 14, 16, 16) // Wall
    );

    public CopperTunBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, com.github.mczme.arsastra.registry.AABlockEntities.COPPER_TUN.get(), CopperTunBlockEntity::serverTick);
    }
}
