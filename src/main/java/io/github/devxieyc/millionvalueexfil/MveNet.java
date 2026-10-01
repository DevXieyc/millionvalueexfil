// Million Value Exfil - networking
package io.github.devxieyc.millionvalueexfil;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MveNet {
    private MveNet() {}

    public record EntryData(UUID id, int slot, int state, ItemStack stack) {
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryData> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, EntryData::id,
                ByteBufCodecs.VAR_INT, EntryData::slot,
                ByteBufCodecs.VAR_INT, EntryData::state,
                ItemStack.OPTIONAL_STREAM_CODEC, EntryData::stack,
                EntryData::new
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, List<EntryData>> LIST_CODEC =
                CODEC.apply(ByteBufCodecs.list(512));
    }

    private record ViewMeta(UUID txnId, UUID ownerId, int version, int mode,
                            Component deathMessage, boolean hardcore) {
        private static final StreamCodec<RegistryFriendlyByteBuf, ViewMeta> CODEC =
                StreamCodec.<RegistryFriendlyByteBuf, ViewMeta, UUID, UUID, Integer, Integer, Component, Boolean>composite(
                        UUIDUtil.STREAM_CODEC, ViewMeta::txnId,
                        UUIDUtil.STREAM_CODEC, ViewMeta::ownerId,
                        ByteBufCodecs.VAR_INT, ViewMeta::version,
                        ByteBufCodecs.VAR_INT, ViewMeta::mode,
                        ComponentSerialization.TRUSTED_STREAM_CODEC, ViewMeta::deathMessage,
                        ByteBufCodecs.BOOL, ViewMeta::hardcore,
                        ViewMeta::new
                );
    }

    private record ViewState(int ticksLeft, int xpTotal, int xpFee, int xpRemain, int score) {
        private static final StreamCodec<RegistryFriendlyByteBuf, ViewState> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ViewState::ticksLeft,
                ByteBufCodecs.VAR_INT, ViewState::xpTotal,
                ByteBufCodecs.VAR_INT, ViewState::xpFee,
                ByteBufCodecs.VAR_INT, ViewState::xpRemain,
                ByteBufCodecs.VAR_INT, ViewState::score,
                ViewState::new
        );
    }

    public record ViewPayload(UUID txnId, UUID ownerId, int version, int mode, int ticksLeft,
                              int xpTotal, int xpFee, int xpRemain, Component deathMessage,
                              boolean hardcore, int score, List<EntryData> entries) implements CustomPacketPayload {
        public static final int MODE_DEATH = 0;
        public static final int MODE_PVP = 1;
        public static final Type<ViewPayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Mve.MODID, "view"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ViewPayload> CODEC = StreamCodec.composite(
                ViewMeta.CODEC,
                v -> new ViewMeta(v.txnId, v.ownerId, v.version, v.mode, v.deathMessage, v.hardcore),
                ViewState.CODEC,
                v -> new ViewState(v.ticksLeft, v.xpTotal, v.xpFee, v.xpRemain, v.score),
                EntryData.LIST_CODEC,
                ViewPayload::entries,
                (meta, state, entries) -> new ViewPayload(meta.txnId(), meta.ownerId(), meta.version(), meta.mode(),
                        state.ticksLeft(), state.xpTotal(), state.xpFee(), state.xpRemain(), meta.deathMessage(),
                        meta.hardcore(), state.score(), entries)
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PreparePayload() implements CustomPacketPayload {
        public static final Type<PreparePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Mve.MODID, "prepare"));
        public static final StreamCodec<ByteBuf, PreparePayload> CODEC = StreamCodec.unit(new PreparePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SelectPayload(UUID txnId, int version, UUID entryId, boolean reserve) implements CustomPacketPayload {
        public static final Type<SelectPayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Mve.MODID, "select"));
        public static final StreamCodec<ByteBuf, SelectPayload> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, SelectPayload::txnId,
                ByteBufCodecs.VAR_INT, SelectPayload::version,
                UUIDUtil.STREAM_CODEC, SelectPayload::entryId,
                ByteBufCodecs.BOOL, SelectPayload::reserve,
                SelectPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClosePayload(UUID ownerId) implements CustomPacketPayload {
        public static final Type<ClosePayload> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(Mve.MODID, "close"));
        public static final StreamCodec<ByteBuf, ClosePayload> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, ClosePayload::ownerId,
                ClosePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(ViewPayload.TYPE, ViewPayload.CODEC,
                    (payload, context) -> io.github.devxieyc.millionvalueexfil.client.MveClient.handleView(payload));
            registrar.playToClient(PreparePayload.TYPE, PreparePayload.CODEC,
                    (payload, context) -> io.github.devxieyc.millionvalueexfil.client.MveClient.handlePrepare());
            registrar.playToClient(ClosePayload.TYPE, ClosePayload.CODEC,
                    (payload, context) -> io.github.devxieyc.millionvalueexfil.client.MveClient.handleClose(payload));
        }
        registrar.playToServer(SelectPayload.TYPE, SelectPayload.CODEC,
                (payload, context) -> TxManager.handleSelect((ServerPlayer) context.player(), payload));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
