package io.github.apace100.apoli;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.networking.ModPacketsS2C;
import io.github.apace100.apoli.networking.UseActivePowersPacket;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.condition.EntityConditionsClient;
import io.github.apace100.apoli.power.factory.condition.ItemConditionsClient;
import io.github.apace100.apoli.registry.ApoliClassDataClient;
import io.github.apace100.apoli.screen.GameHudRender;
import io.github.apace100.apoli.screen.PowerHudRenderer;
import io.github.apace100.apoli.util.ApoliConfigClient;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import xyz.bluspring.originslegacy.platform.PlatformNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/** Client entrypoint; invoked by xyz.bluspring.originslegacy.OriginsLegacyClientMod. */
public class ApoliClient {

	public static boolean shouldReloadWorldRenderer = false;

	private static HashMap<String, KeyMapping> idToKeyBindingMap = new HashMap<>();
	private static HashMap<String, Boolean> lastKeyBindingStates = new HashMap<>();
	private static boolean initializedKeyBindingMap = false;

	public static void registerPowerKeybinding(String keyId, KeyMapping keyBinding) {
		idToKeyBindingMap.put(keyId, keyBinding);
	}

	private static boolean initialized;

	public void onInitializeClient() {
		if(initialized) return;
		initialized = true;

		ModPacketsS2C.register();

		ApoliClassDataClient.registerAll();

		EntityConditionsClient.register();
		ItemConditionsClient.register();
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> {
			Minecraft tick = Minecraft.getInstance();
			if(tick.player != null) {
				List<Power> powers = PowerHolderComponent.KEY.get(tick.player).getPowers();
				List<Power> pressedPowers = new LinkedList<>();
				HashMap<String, Boolean> currentKeyBindingStates = new HashMap<>();
				for(Power power : powers) {
					if(power instanceof Active) {
						Active active = (Active)power;
						Active.Key key = active.getKey();
						KeyMapping keyBinding = getKeyBinding(key.key);
						if(keyBinding != null) {
							if(!currentKeyBindingStates.containsKey(key.key)) {
								currentKeyBindingStates.put(key.key, keyBinding.isDown());
							}
							if(currentKeyBindingStates.get(key.key) && (key.continuous || !lastKeyBindingStates.getOrDefault(key.key, false))) {
								pressedPowers.add(power);
							}
						}
					}
				}
				lastKeyBindingStates = currentKeyBindingStates;
				if(pressedPowers.size() > 0) {
					performActivePowers(pressedPowers);
				}
			}
		});

		GameHudRender.HUD_RENDERS.add(new PowerHudRenderer());

		AutoConfig.register(ApoliConfigClient.class, JanksonConfigSerializer::new);
		Apoli.config = AutoConfig.getConfigHolder(ApoliConfigClient.class).getConfig();
	}

	private void performActivePowers(List<Power> powers) {
		for(Power power : powers) {
			((Active) power).onUse();
		}
		PlatformNetworking.sendToServer(new UseActivePowersPacket((List<PowerType<?>>) (Object) powers.stream().map(p -> p.getType()).toList()));
	}

	private KeyMapping getKeyBinding(String key) {
		if(!idToKeyBindingMap.containsKey(key)) {
			if(!initializedKeyBindingMap) {
				initializedKeyBindingMap = true;
				Minecraft client = Minecraft.getInstance();
				for(int i = 0; i < client.options.keyMappings.length; i++) {
					idToKeyBindingMap.put(client.options.keyMappings[i].getName(), client.options.keyMappings[i]);
				}
				return getKeyBinding(key);
			}
			return null;
		}
		return idToKeyBindingMap.get(key);
	}
}
