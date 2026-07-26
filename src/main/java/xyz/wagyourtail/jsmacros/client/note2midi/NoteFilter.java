package xyz.wagyourtail.jsmacros.client.note2midi;

public final class NoteFilter {
    private NoteFilter() {
    }

    public static boolean acceptsSoundId(String soundId) {
        return CapturedNote.isNoteBlockSound(soundId);
    }

    public static boolean withinRadius(double noteX, double noteY, double noteZ,
                                       double centerX, double centerY, double centerZ, double radius) {
        if (radius < 0.0 || !Double.isFinite(radius)) return false;
        double dx = noteX - centerX;
        double dy = noteY - centerY;
        double dz = noteZ - centerZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}
