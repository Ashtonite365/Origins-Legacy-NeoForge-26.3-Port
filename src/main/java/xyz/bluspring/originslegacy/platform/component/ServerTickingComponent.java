/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

/** Ticked on the logical server after the holder entity ticks (CCA ServerTickingComponent). */
public interface ServerTickingComponent {
    void serverTick();
}
