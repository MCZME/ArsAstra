package com.github.mczme.arsastra.block;

import com.github.mczme.arsastra.block.entity.AbstractTunBlockEntity;
import com.github.mczme.arsastra.block.entity.CopperTunBlockEntity;
import com.github.mczme.arsastra.registry.AABlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CopperTunBlock extends AbstractTunBlock {
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperTunBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<? extends AbstractTunBlockEntity> getBlockEntityType() {
        return AABlockEntities.COPPER_TUN.get();
    }
}