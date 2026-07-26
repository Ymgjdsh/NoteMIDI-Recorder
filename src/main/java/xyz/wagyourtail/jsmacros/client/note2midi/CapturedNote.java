package xyz.wagyourtail.jsmacros.client.note2midi;

import java.util.Objects;

public final class CapturedNote {
    public static final String NOTE_BLOCK_PREFIX = "minecraft:block.note_block.";

    private final String fullSoundId;
    private final String instrument;
    private final float pitch;
    private final float volume;
    private final double x;
    private final double y;
    private final double z;
    private final long nanoTime;
    private final long clientTick;
    private final String serverId;
    private final String dimensionId;

    public CapturedNote(String fullSoundId, float pitch, float volume, double x, double y, double z,
                        long nanoTime, long clientTick, String serverId, String dimensionId) {
        this.fullSoundId = Objects.requireNonNull(fullSoundId, "fullSoundId");
        this.instrument = instrumentSuffix(fullSoundId);
        this.pitch = pitch;
        this.volume = volume;
        this.x = x;
        this.y = y;
        this.z = z;
        this.nanoTime = nanoTime;
        this.clientTick = clientTick;
        this.serverId = serverId == null ? "unknown-server" : serverId;
        this.dimensionId = dimensionId == null ? "unknown-dimension" : dimensionId;
    }

    public static boolean isNoteBlockSound(String soundId) {
        return soundId != null && soundId.startsWith(NOTE_BLOCK_PREFIX)
                && soundId.length() > NOTE_BLOCK_PREFIX.length();
    }

    public static String instrumentSuffix(String soundId) {
        return isNoteBlockSound(soundId) ? soundId.substring(NOTE_BLOCK_PREFIX.length()) : "unknown";
    }

    public String getFullSoundId() { return fullSoundId; }
    public String getInstrument() { return instrument; }
    public float getPitch() { return pitch; }
    public float getVolume() { return volume; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public long getNanoTime() { return nanoTime; }
    public long getClientTick() { return clientTick; }
    public String getServerId() { return serverId; }
    public String getDimensionId() { return dimensionId; }
}
