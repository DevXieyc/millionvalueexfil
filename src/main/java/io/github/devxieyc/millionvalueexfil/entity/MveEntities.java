// Million Value Exfil - entity registrations
package io.github.devxieyc.millionvalueexfil.entity;

import io.github.devxieyc.millionvalueexfil.Mve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class MveEntities {
    private MveEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Mve.MODID);

    public static final Supplier<EntityType<DebugCorpseEntity>> DEBUG_CORPSE =
            TYPES.register("debug_corpse", () -> EntityType.Builder
                    .<DebugCorpseEntity>of(DebugCorpseEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.8F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(10)
                    .build("debug_corpse"));
}
