package xyz.wagyourtail.jsmacros.client.note2midi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FilenameUtil {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private FilenameUtil() {
    }

    public static String sanitizeComponent(String value) {
        String source = value == null ? "unknown" : value;
        String sanitized = source.replaceAll("[\\x00-\\x1f<>:\"/\\\\|?*]+", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("[. ]+$", "")
                .replaceAll("_+", "_");
        if (sanitized.isEmpty()) sanitized = "unknown";
        return sanitized.length() > 80 ? sanitized.substring(0, 80) : sanitized;
    }

    public static String recordingBaseName(LocalDateTime time, String server, String dimension) {
        return "note2midi-" + TIMESTAMP.format(time) + "-" + sanitizeComponent(server)
                + "-" + sanitizeComponent(dimension);
    }

    public static Path reserveUniqueMidiFile(Path directory, String baseName) throws IOException {
        Files.createDirectories(directory);
        String cleanBase = sanitizeComponent(baseName);
        for (int index = 1; index < 10_000; index++) {
            String suffix = index == 1 ? "" : "-" + index;
            Path candidate = directory.resolve(cleanBase + suffix + ".mid").toAbsolutePath();
            try {
                return Files.createFile(candidate);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
            }
        }
        throw new IOException("Could not reserve a unique MIDI file in " + directory);
    }
}
