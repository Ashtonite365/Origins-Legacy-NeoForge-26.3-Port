package io.github.apace100.calio.resource;

import xyz.bluspring.originslegacy.platform.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @deprecated  Deprecated in favour of using Fabric's IdentifiableResourceReloadListener.
 *              To establish an `after` dependency, simply add the other reload listener's identifier
 *              (IdentifiableResourceReloadListener#getFabricId) to the set your reload listener returns
 *              in IdentifiableResourceReloadListener#getFabricDependencies.
 *              To establish a `before` dependency, the other resource loader needs to expose the dependency
 *              set it returns in that method, e.g. by using a publicly accessible set, or exposing a public
 *              method to add to it.
 */
@Deprecated
public class OrderedResourceListener {

    public static final String ENTRYPOINT_KEY = "calio:ordered-resource-listener";

    /** NeoForge port: replaces the Fabric "calio:ordered-resource-listener" entrypoint list. */
    private static final java.util.List<OrderedResourceListenerInitializer> INITIALIZERS = new java.util.ArrayList<>();
    private static boolean finished;

    /** Register an initializer (formerly a fabric.mod.json entrypoint). Call during mod construction. */
    public static synchronized void registerInitializer(OrderedResourceListenerInitializer initializer) {
        if (finished) throw new IllegalStateException("Ordered resource listeners already finalized");
        INITIALIZERS.add(initializer);
    }

    /** Kept for OriginsLegacyMod's call order; actual sorting is deferred to {@link #finishIfNeeded()}. */
    public void onInitialize() {}

    /** Runs all initializers once and sorts the listeners. Called lazily by CalioReloadListeners on the first reload event. */
    static synchronized void finishIfNeeded() {
        if (finished) return;
        finished = true;
        OrderedResourceListenerManager manager = new OrderedResourceListenerManager();
        INITIALIZERS.forEach(entrypoint -> entrypoint.registerResourceListeners(manager));
        manager.finishRegistration();
    }

    public static class Registration {

        private final OrderedResourceListenerManager.Instance manager;
        final Identifier id;
        final IdentifiableResourceReloadListener resourceReloadListener;
        final Function<HolderLookup.Provider, ? extends PreparableReloadListener> reloadListenerProvider;
        final Set<Identifier> dependencies = new HashSet<>();
        final Set<Identifier> dependants = new HashSet<>();
        private boolean isCompleted;

        Registration(OrderedResourceListenerManager.Instance manager, IdentifiableResourceReloadListener listener) {
            this.id = listener.getFabricId();
            this.manager = manager;
            this.resourceReloadListener = listener;
            this.reloadListenerProvider = null;
        }

        Registration(Identifier id, OrderedResourceListenerManager.Instance manager, Function<HolderLookup.Provider, ? extends PreparableReloadListener> listenerProvider) {
            this.id = id;
            this.manager = manager;
            this.resourceReloadListener = null;
            this.reloadListenerProvider = listenerProvider;
        }

        public Registration after(String identifier) {
            return after(Identifier.parse(identifier));
        }

        public Registration after(Identifier identifier) {
            if(isCompleted) {
                throw new IllegalStateException(
                    "Can't add a resource reload listener registration dependency after it was completed.");
            }
            dependencies.add(identifier);
            return this;
        }

        public Registration before(String identifier) {
            return before(Identifier.parse(identifier));
        }

        public Registration before(Identifier identifier) {
            if(isCompleted) {
                throw new IllegalStateException(
                    "Can't add a resource reload listener registration dependant after it was completed.");
            }
            dependants.add(identifier);
            return this;
        }

        public void complete() {
            isCompleted = true;
            manager.add(this);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(id.toString());
            builder.append("{depends_on=[");
            boolean first = true;
            for (Identifier afterId : dependencies) {
                builder.append(afterId);
                if(!first) {
                    builder.append(',');
                }
                first = false;
            }
            builder.append("]}");
            return builder.toString();
        }
    }
}
