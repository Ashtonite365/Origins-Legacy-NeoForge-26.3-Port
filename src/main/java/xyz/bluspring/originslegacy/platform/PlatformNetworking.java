/*
 * Origins: Legacy personal NeoForge port - platform layer (replaces Fabric API, CCA, AEA, ModMenu usages).
 * Part of the port of Origins: Legacy / Apoli-Legacy / Calio-Legacy, MIT, (c) 2020-2021 apace100 (see LICENSE*).
 */
package xyz.bluspring.originslegacy.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Play-phase networking (replaces PayloadTypeRegistry, ServerPlayNetworking, ClientPlayNetworking, PlayerLookup).
 *
 * <ul>
 *   <li>{@link #registerClientbound(CustomPacketPayload.Type, StreamCodec)} / {@link #registerServerbound} during mod construction
 *       (i.e. from the old {@code onInitialize}). Replaces {@code PayloadTypeRegistry.clientboundPlay()/serverboundPlay().register}.</li>
 *   <li>{@link #registerServerReceiver} replaces {@code ServerPlayNetworking.registerGlobalReceiver}.</li>
 *   <li>{@link #registerClientReceiver} replaces {@code ClientPlayNetworking.registerGlobalReceiver}; call it from client-only code.</li>
 * </ul>
 * Handlers run on the main thread (NeoForge default), so {@code context.server().execute(..)} wrappers can be dropped.
 */
public final class PlatformNetworking {
    private PlatformNetworking() {}

    /** Handler with Fabric-like (payload, context) shape. {@link IPayloadContext#player()} is the sender (server) or local player (client). */
    @FunctionalInterface
    public interface Handler<T extends CustomPacketPayload> {
        void handle(T payload, IPayloadContext context);
    }

    private record Reg<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, boolean toClient) {}

    private static final List<Reg<?>> REGS = new ArrayList<>();
    private static final Map<CustomPacketPayload.Type<?>, Handler<?>> SERVER_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<CustomPacketPayload.Type<?>, Handler<?>> CLIENT_HANDLERS = new ConcurrentHashMap<>();

    static void init(IEventBus bus) {
        bus.addListener(RegisterPayloadHandlersEvent.class, PlatformNetworking::onRegister);
    }

    public static <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        REGS.add(new Reg<>(type, codec, true));
    }

    public static <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        REGS.add(new Reg<>(type, codec, false));
    }

    public static <T extends CustomPacketPayload> void registerServerReceiver(CustomPacketPayload.Type<T> type, Handler<T> handler) {
        SERVER_HANDLERS.put(type, handler);
    }

    /** Client side only. May be called any time (also after RegisterPayloadHandlersEvent). */
    public static <T extends CustomPacketPayload> void registerClientReceiver(CustomPacketPayload.Type<T> type, Handler<T> handler) {
        CLIENT_HANDLERS.put(type, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Platform.MOD_ID).versioned("1").optional();
        for (Reg r : REGS) {
            CustomPacketPayload.Type type = r.type();
            if (r.toClient()) {
                registrar.playToClient(type, r.codec(), (payload, ctx) -> dispatch(CLIENT_HANDLERS, type, (CustomPacketPayload) payload, ctx));
            } else {
                registrar.playToServer(type, r.codec(), (payload, ctx) -> dispatch(SERVER_HANDLERS, type, (CustomPacketPayload) payload, ctx));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void dispatch(Map<CustomPacketPayload.Type<?>, Handler<?>> map, CustomPacketPayload.Type<?> type, CustomPacketPayload payload, IPayloadContext ctx) {
        Handler h = map.get(type);
        if (h != null) h.handle(payload, ctx);
    }

    // ---- sending (replaces ServerPlayNetworking.send / ClientPlayNetworking.send / PlayerLookup) ----

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
    public static void sendToAll(CustomPacketPayload payload) { PacketDistributor.sendToAllPlayers(payload); }
    public static void sendToTracking(Entity entity, CustomPacketPayload payload) { PacketDistributor.sendToPlayersTrackingEntity(entity, payload); }
    public static void sendToTrackingAndSelf(Entity entity, CustomPacketPayload payload) { PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload); }
    public static void sendToDimension(ServerLevel level, CustomPacketPayload payload) { PacketDistributor.sendToPlayersInDimension(level, payload); }

    /** Client side only: replaces ClientPlayNetworking.send. */
    public static void sendToServer(CustomPacketPayload payload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }

    /** Replaces PlayerLookup.tracking(entity): players tracking the entity (excluding itself). */
    public static Collection<ServerPlayer> tracking(Entity entity) {
        List<ServerPlayer> out = new ArrayList<>();
        if (entity.level() instanceof ServerLevel level) {
            out.addAll(level.getChunkSource().chunkMap.getPlayersWatching(entity));
            out.remove(entity);
        }
        return out;
    }

    /** Replaces PlayerLookup.all(server). */
    public static Collection<ServerPlayer> all(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers();
    }

    /** Runs {@code action} for every player tracking {@code entity} plus the entity itself if it's a player. */
    public static void forTrackingAndSelf(Entity entity, Consumer<ServerPlayer> action) {
        tracking(entity).forEach(action);
        if (entity instanceof ServerPlayer sp) action.accept(sp);
    }
}
