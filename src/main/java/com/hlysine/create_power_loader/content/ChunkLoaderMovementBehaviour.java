package com.hlysine.create_power_loader.content;

import com.hlysine.create_power_loader.config.CPLConfigs;
import com.hlysine.create_power_loader.content.andesitechunkloader.AndesiteChunkLoaderRenderer;
import com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderRenderer;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class ChunkLoaderMovementBehaviour implements MovementBehaviour {
    private static final Logger LOGGER = LogUtils.getLogger();

    public final BehaviorType behaviorType;

    public ChunkLoaderMovementBehaviour(BehaviorType type) {
        this.behaviorType = type;
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.world.isClientSide || !(context.world instanceof ServerLevel))
            return;
        if (context.contraption.entity == null)
            return;

        ChunkPos entityChunkPosition = context.contraption.entity.chunkPosition();
        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState)) {
            tempState = new SavedState(entityChunkPosition, new HashSet<>());
            context.temporaryData = tempState;
        }

        SavedState savedState = (SavedState) tempState;

        if (entityChunkPosition.equals(savedState.chunkPos))
            return;

        savedState.chunkPos = entityChunkPosition;

        if (shouldFunction()) {
            ChunkLoadingUtils.updateForcedChunks((ServerLevel) context.world, entityChunkPosition, context.contraption.entity.getUUID(), 2, savedState.forcedChunks);
            LOGGER.debug("CPL: Entity {} at new chunk {}, loaded {} chunks", context.contraption.entity, entityChunkPosition, savedState.forcedChunks.size());
        } else {
            ChunkLoadingUtils.unforceAllChunks((ServerLevel) context.world, context.contraption.entity.getUUID(), savedState.forcedChunks);
        }

        context.temporaryData = savedState;
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide || !(context.world instanceof ServerLevel))
            return;
        if (context.contraption.entity == null)
            return;

        ChunkPos entityChunkPosition = context.contraption.entity.chunkPosition();
        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState)) {
            tempState = new SavedState(entityChunkPosition, new HashSet<>());
            context.temporaryData = tempState;

            SavedState savedState = (SavedState) tempState;

            Set<ChunkPos> savedForcedChunks = ChunkLoadingUtils.getSavedForcedChunks(context.contraption.entity.getUUID());
            if (savedForcedChunks != null) {
                ((SavedState) tempState).forcedChunks.addAll(savedForcedChunks);
                LOGGER.debug("CPL: Entity {} reclaimed {} chunks", context.contraption.entity, savedForcedChunks.size());
            }

            if (shouldFunction()) {
                ChunkLoadingUtils.updateForcedChunks((ServerLevel) context.world, entityChunkPosition, context.contraption.entity.getUUID(), 2, savedState.forcedChunks);
                LOGGER.debug("CPL: Entity {} starts moving at chunk {}, loaded {} chunks", context.contraption.entity, entityChunkPosition, savedState.forcedChunks.size());
            } else
                ChunkLoadingUtils.unforceAllChunks((ServerLevel) context.world, context.contraption.entity.getUUID(), savedState.forcedChunks);
            savedState.chunkPos = entityChunkPosition;
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (context.world.isClientSide || !(context.world instanceof ServerLevel))
            return;
        if (context.contraption.entity == null)
            return;

        Object tempState = context.temporaryData;

        if (!(tempState instanceof SavedState))
            return;

        SavedState savedState = (SavedState) tempState;

        if (shouldFunction()) // no need to log if we don't expect it to function
            LOGGER.debug("CPL: Entity {} stops moving, unloaded {} chunks", context.contraption.entity, savedState.forcedChunks.size());
        ChunkLoadingUtils.unforceAllChunks((ServerLevel) context.world, context.contraption.entity.getUUID(), savedState.forcedChunks);

        context.temporaryData = null;
    }

    @Override
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (behaviorType == BehaviorType.ANDESITE) {
            AndesiteChunkLoaderRenderer.renderInContraption(context, renderWorld, matrices, buffer);
        } else if (behaviorType == BehaviorType.BRASS) {
            BrassChunkLoaderRenderer.renderInContraption(context, renderWorld, matrices, buffer);
        } else {
            throw new RuntimeException("Unknown BehaviorType. This should not be reachable.");
        }
    }

    private boolean shouldFunction() {
        if (behaviorType == BehaviorType.ANDESITE) {
            return CPLConfigs.server().andesiteOnContraption.get();
        } else if (behaviorType == BehaviorType.BRASS) {
            return CPLConfigs.server().brassOnContraption.get();
        } else {
            return false;
        }
    }

    static class SavedState {
        public ChunkPos chunkPos;
        public Set<ChunkPos> forcedChunks;

        public SavedState(ChunkPos chunkPos, Set<ChunkPos> forcedChunks) {
            this.chunkPos = chunkPos;
            this.forcedChunks = forcedChunks;
        }
    }

    public enum BehaviorType {
        ANDESITE,
        BRASS
    }
}
