package xyz.wagyourtail.jsmacros.client.note2midi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilenameUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void sanitizesWindowsIllegalCharactersAndTrailingDots() {
        assertEquals("server_25565_overworld", FilenameUtil.sanitizeComponent("server:25565/overworld..."));
    }

    @Test
    void reservesWithoutOverwriting() throws Exception {
        Path first = FilenameUtil.reserveUniqueMidiFile(tempDir, "recording");
        Path second = FilenameUtil.reserveUniqueMidiFile(tempDir, "recording");
        assertNotEquals(first, second);
        assertTrue(first.toFile().exists());
        assertTrue(second.toFile().exists());
    }
}
