/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import xyz.bluspring.originslegacy.platform.PlatformRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * CCA ComponentKey replacement backed by a NeoForge {@link AttachmentType}. Create once in a static field with
 * {@link #create}; the attachment type is registered automatically. Keys must exist before RegisterEvent
 * (mod construction); OriginsLegacyMod bootstraps the core keys.
 * <p>Usage as with CCA: {@code KEY.get(entity)}, {@code KEY.maybeGet(entity)}, {@code KEY.sync(entity)}.
 */
public final class ComponentKey<C extends PlatformComponent> {
    private static final Map<Identifier, ComponentKey<?>> BY_ID = new ConcurrentHashMap<>();

    private final Identifier id;
    private final Class<C> componentClass;
    private final Class<? extends Entity> holderClass;
    private final RespawnCopyStrategy respawnStrategy;
    private final AttachmentType<C> attachment;

    private <E extends Entity> ComponentKey(Identifier id, Class<C> componentClass, Class<E> holderClass,
                                           Function<? super E, ? extends C> factory, RespawnCopyStrategy strategy) {
        this.id = id;
        this.componentClass = componentClass;
        this.holderClass = holderClass;
        this.respawnStrategy = strategy;
        Function<IAttachmentHolder, C> ctor = holder -> {
            if (!holderClass.isInstance(holder)) {
                throw new IllegalStateException("Component " + id + " is not provided by " + holder);
            }
            return factory.apply(holderClass.cast(holder));
        };
        AttachmentType.Builder<C> builder = AttachmentType.builder(ctor).serialize(new IAttachmentSerializer<C>() {
            @Override
            public C read(IAttachmentHolder holder, ValueInput input) {
                C c = ctor.apply(holder);
                c.readData(input);
                return c;
            }

            @Override
            public boolean write(C component, ValueOutput output) {
                component.writeData(output);
                return true;
            }
        });
        switch (strategy) {
            case ALWAYS_COPY, CHARACTER -> builder.copyOnDeath();
            case INVENTORY -> builder.copyOnDeath().copyHandler((component, holder, provider) -> {
                if (holder instanceof Entity e && e.level() instanceof ServerLevel sl && sl.getGameRules().get(GameRules.KEEP_INVENTORY)) {
                    TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
                    component.writeData(out);
                    C copy = ctor.apply(holder);
                    copy.readData(TagValueInput.create(ProblemReporter.DISCARDING, provider, out.buildResult()));
                    return copy;
                }
                return null;
            });
            case NEVER_COPY -> { }
        }
        this.attachment = builder.build();
    }

    /**
     * Create and register a key (replaces ComponentRegistry.getOrCreate + EntityComponentInitializer registration).
     * @param holderClass entities that get the component (e.g. LivingEntity.class, Player.class)
     * @param factory     creates the component for an entity
     * @param strategy    player respawn copy behaviour
     */
    public static <C extends PlatformComponent, E extends Entity> ComponentKey<C> create(Identifier id, Class<C> componentClass,
            Class<E> holderClass, Function<? super E, ? extends C> factory, RespawnCopyStrategy strategy) {
        ComponentKey<C> key = new ComponentKey<>(id, componentClass, holderClass, factory, strategy);
        if (BY_ID.putIfAbsent(id, key) != null) throw new IllegalStateException("Duplicate component key " + id);
        PlatformRegistries.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, id, key.attachment);
        return key;
    }

    public Identifier getId() { return id; }
    public Class<C> getComponentClass() { return componentClass; }
    public RespawnCopyStrategy getRespawnStrategy() { return respawnStrategy; }
    public AttachmentType<C> attachmentType() { return attachment; }

    public static Collection<ComponentKey<?>> all() { return Collections.unmodifiableCollection(BY_ID.values()); }
    public static Optional<ComponentKey<?>> byId(Identifier id) { return Optional.ofNullable(BY_ID.get(id)); }

    public boolean isProvidedBy(Object provider) { return holderClass.isInstance(provider); }

    /** The component (created on first access). Throws NoSuchElementException if the object can't hold it. */
    public C get(Object provider) {
        if (!isProvidedBy(provider)) throw new NoSuchElementException(provider + " does not provide component " + id);
        return ((Entity) provider).getData(attachment);
    }

    public Optional<C> maybeGet(Object provider) {
        return isProvidedBy(provider) ? Optional.of(((Entity) provider).getData(attachment)) : Optional.empty();
    }

    /** True if the component already exists on the entity (doesn't create it). */
    public boolean isPresent(Object provider) {
        return isProvidedBy(provider) && ((Entity) provider).hasData(attachment);
    }

    /** Server side: send to all players tracking the entity plus the entity itself if it's a player. No-op on the client. */
    public void sync(Object provider) {
        if (!(provider instanceof Entity entity) || entity.level().isClientSide() || !isProvidedBy(provider)) return;
        C component = get(entity);
        PlatformNetworking.forTrackingAndSelf(entity, p -> syncWith(p, entity, component));
    }

    /** Server side: send the component of {@code provider} to one player. */
    public void syncWith(ServerPlayer player, Object provider) {
        if (provider instanceof Entity entity && isProvidedBy(entity)) syncWith(player, entity, get(entity));
    }

    private void syncWith(ServerPlayer player, Entity entity, C component) {
        if (!component.shouldSyncWith(player)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), entity.registryAccess());
        try {
            component.writeSyncPacket(buf, player);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            PlatformNetworking.sendToPlayer(player, new ComponentSyncPayload(entity.getId(), id, bytes));
        } finally {
            buf.release();
        }
    }

    void applySync(Entity entity, byte[] data) {
        if (!isProvidedBy(entity)) return;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), entity.registryAccess());
        try {
            get(entity).applySyncPacket(buf);
        } finally {
            buf.release();
        }
    }
}
