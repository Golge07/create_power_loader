package com.hlysine.create_power_loader.content.brasschunkloader;

import com.hlysine.create_power_loader.CPLBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class BrassChunkLoaderBlock extends DirectionalKineticBlock implements IBE<BrassChunkLoaderBlockEntity> {

    public BrassChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState pState) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAnalogOutputSignal(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos) {
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof BrassChunkLoaderBlockEntity chunkLoader) {
            return chunkLoader.isSpeedRequirementFulfilled() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public Class<BrassChunkLoaderBlockEntity> getBlockEntityClass() {
        return BrassChunkLoaderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BrassChunkLoaderBlockEntity> getBlockEntityType() {
        return CPLBlockEntityTypes.BRASS_CHUNK_LOADER.get();
    }

}
