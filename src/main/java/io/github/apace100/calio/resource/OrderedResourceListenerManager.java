package io.github.apace100.calio.resource;

import com.google.common.collect.Lists;
import xyz.bluspring.originslegacy.platform.IdentifiableResourceReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
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
public class OrderedResourceListenerManager {
    /** NeoForge port: registry-based providers, in dependency order; consumed by {@link CalioReloadListeners}. */
    private static final List<CalioReloadListeners.Entry> registryBasedReloadListenerProviders = new ArrayList<>();

    public static List<CalioReloadListeners.Entry> getRegistryBasedReloadListenerProviders() {
        return registryBasedReloadListenerProviders;
    }

    private final Instance registryBasedInstance = new Instance(CalioReloadListeners::queueServer, null);
    private final HashMap<PackType, Instance> instances = new HashMap<>();

    OrderedResourceListenerManager() {}

    public OrderedResourceListener.Registration register(PackType resourceType, IdentifiableResourceReloadListener resourceReloadListener) {
        Instance inst = instances.computeIfAbsent(resourceType, rt -> new Instance(rt == PackType.CLIENT_RESOURCES ? CalioReloadListeners::queueClient : CalioReloadListeners::queueServer));
        return new OrderedResourceListener.Registration(inst, resourceReloadListener);
    }

    public OrderedResourceListener.Registration registerWithRegistries(Identifier id, Function<HolderLookup.Provider, ? extends PreparableReloadListener> reloadListener) {
        return new OrderedResourceListener.Registration(id, registryBasedInstance, reloadListener);
    }

    void finishRegistration() {
        for(Instance inst : instances.values()) {
            inst.finish();
        }

        registryBasedInstance.finish();
    }

    static class Instance {
        private final HashMap<Identifier, OrderedResourceListener.Registration> registrations = new HashMap<>();
        private final HashMap<Integer, List<Identifier>> sortedMap = new HashMap<>();
        private int maxIndex = 0;

        private final Consumer<IdentifiableResourceReloadListener> registrationMethod;
        private final boolean registryBased;

        private Instance(Consumer<IdentifiableResourceReloadListener> registrationMethod) {
            this.registrationMethod = registrationMethod;
            this.registryBased = false;
        }

        private Instance(Consumer<IdentifiableResourceReloadListener> registrationMethod, Object registryBasedMarker) {
            this.registrationMethod = registrationMethod;
            this.registryBased = true;
        }

        void add(OrderedResourceListener.Registration registration) {
            registrations.put(registration.id, registration);
        }

        void finish() {
            prepareSetsAndSort();
            List<Identifier> sortedList = new LinkedList<>();
            List<Identifier> nextListeners;
            while(!(nextListeners = copy(getRegistrations(0))).isEmpty()) {
                sortedList.addAll(nextListeners);
                sortedMap.remove(0);
                for(int i = 1; i <= maxIndex; i++) {
                    for(Identifier regId : copy(getRegistrations(i))) {
                        OrderedResourceListener.Registration registration = registrations.get(regId);
                        int before = registration.dependencies.size();
                        nextListeners.forEach(registration.dependencies::remove);
                        update(registration, before);
                    }
                }
            }
            if(!sortedMap.isEmpty()) {
                StringBuilder errorBuilder = new StringBuilder("Couldn't resolve ordered resource listener dependencies. Unsolved:");
                for(int i = 0; i <= maxIndex; i++) {
                    if(!getRegistrations(i).isEmpty()) {
                        errorBuilder.append("\t").append(i).append(" dependencies:");
                        for(Identifier id : getRegistrations(i)) {
                            OrderedResourceListener.Registration registration = registrations.get(id);
                            errorBuilder.append("\t\t").append(registration.toString());
                            if (registration.resourceReloadListener != null)
                                registrationMethod.accept(registration.resourceReloadListener);
                            else
                                CalioReloadListeners.queueRegistryBased(registration.id, registration.reloadListenerProvider);
                        }
                    }
                }
                throw new RuntimeException(errorBuilder.toString());
            } else {
                for(Identifier id : sortedList) {
                    OrderedResourceListener.Registration registration = registrations.get(id);
                    if (registration.resourceReloadListener != null)
                        registrationMethod.accept(registration.resourceReloadListener);
                    else
                        CalioReloadListeners.queueRegistryBased(registration.id, registration.reloadListenerProvider);
                }
            }
        }

        private void prepareSetsAndSort() {
            for (OrderedResourceListener.Registration reg : registrations.values()) {
                reg.dependencies.removeIf(id -> !registrations.containsKey(id));
                reg.dependants.forEach(id -> {
                    if(registrations.containsKey(id)) {
                        registrations.get(id).dependencies.add(reg.id);
                    }
                });
            }
            registrations.values().forEach(this::sortIntoMap);
        }

        private void sortIntoMap(OrderedResourceListener.Registration registration) {
            int index = registration.dependencies.size();
            List<Identifier> list = sortedMap.computeIfAbsent(index, i -> new LinkedList<>());
            list.add(registration.id);
            if(index > maxIndex) {
                maxIndex = index;
            }
        }

        private void update(OrderedResourceListener.Registration registration, int indexBefore) {
            int index = registration.dependencies.size();
            if(index == indexBefore) {
                return;
            }
            List<Identifier> regs = getRegistrations(indexBefore);
            regs.remove(registration.id);
            if(regs.isEmpty()) {
                sortedMap.remove(indexBefore);
            }
            List<Identifier> list = sortedMap.computeIfAbsent(index, i -> new LinkedList<>());
            list.add(registration.id);
        }

        private List<Identifier> getRegistrations(int index) {
            return sortedMap.getOrDefault(index, new LinkedList<>());
        }
    }

    private static <T> List<T> copy(List<T> list) {
        return Lists.newLinkedList(list);
    }
}
