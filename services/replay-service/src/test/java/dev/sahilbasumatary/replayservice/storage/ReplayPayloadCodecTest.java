package dev.sahilbasumatary.replayservice.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sahilbasumatary.replayservice.exception.ReplayStorageException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ReplayPayloadCodecTest {

    private final ReplayPayloadCodec codec = new ReplayPayloadCodec();

    @Test
    void compressThenDecompressRoundTrips() {
        byte[] original = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

        CompressedPayload payload = codec.compress(original);
        byte[] restored = codec.decompress(payload.data(), payload.checksumSha256());

        assertArrayEquals(original, restored);
        assertEquals(original.length, payload.uncompressedBytes());
    }

    @Test
    void repetitivePayloadShrinks() {
        byte[] repetitive = "frame".repeat(1000).getBytes(StandardCharsets.UTF_8);

        CompressedPayload payload = codec.compress(repetitive);

        assertTrue(
                payload.sizeBytes() < repetitive.length,
                "compressed size " + payload.sizeBytes() + " should beat " + repetitive.length);
    }

    @Test
    void checksumIsStableForSameInput() {
        byte[] data = "deterministic".getBytes(StandardCharsets.UTF_8);

        assertEquals(codec.checksum(data), codec.checksum(data));
    }

    @Test
    void decompressRejectsChecksumMismatch() {
        CompressedPayload payload =
                codec.compress("payload".getBytes(StandardCharsets.UTF_8));

        String wrongChecksum = "0".repeat(64);

        assertThrows(
                ReplayStorageException.class,
                () -> codec.decompress(payload.data(), wrongChecksum));
    }
}
