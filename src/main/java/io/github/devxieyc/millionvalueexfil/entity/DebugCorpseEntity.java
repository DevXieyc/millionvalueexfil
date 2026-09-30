// Million Value Exfil - debug corpse entity
package io.github.devxieyc.millionvalueexfil.entity;

import io.github.devxieyc.millionvalueexfil.Mve;
import io.github.devxieyc.millionvalueexfil.TxManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class DebugCorpseEntity extends Entity {
    private int lifeTicks = 600;

    public DebugCorpseEntity(EntityType<? extends DebugCorpseEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setLifeTicks(int ticks) {
        this.lifeTicks = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (--this.lifeTicks <= 0) expire();
    }

    public void expire() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Mve.LOGGER.info("[MVE-DEBUG] corpse expired, dropping leftovers");
            TxManager.expireDummy(this.getUUID(), serverLevel);
        }
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
