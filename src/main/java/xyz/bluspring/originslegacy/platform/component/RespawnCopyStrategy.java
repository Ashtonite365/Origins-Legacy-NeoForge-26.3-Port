/*
 * Origins: Legacy personal NeoForge port - platform layer: Cardinal Components API replacement on NeoForge data attachments.
 * Written from scratch against the CCA method names used by Origins/Apoli (no CCA code copied). MIT, see LICENSE*.
 */
package xyz.bluspring.originslegacy.platform.component;

/** Whether a player component survives PlayerEvent.Clone after death (non-death clones, e.g. leaving the End, always copy). */
public enum RespawnCopyStrategy {
    ALWAYS_COPY,
    /** Character data: also copied on death (same as ALWAYS_COPY here). */
    CHARACTER,
    /** Copied on death only with the keep_inventory game rule. */
    INVENTORY,
    /** Never copied on death. */
    NEVER_COPY
}
