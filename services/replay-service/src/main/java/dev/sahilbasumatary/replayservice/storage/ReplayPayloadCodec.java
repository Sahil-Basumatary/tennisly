package dev.sahilbasumatary.replayservice.storage;

import dev.sahilbasumatary.replayservice.exception.ReplayStorageException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Component;

/**
 * Compresses replay payloads with gzip and verifies their integrity via SHA-256. Frame data is
 * highly repetitive, so gzip typically shrinks it by an order of magnitude before it reaches object
 * storage.
 */
@Component
public class ReplayPayloadCodec {

    private static final String DIGEST_ALGORITHM = "SHA-256";

    public CompressedPayload compress(byte[] raw) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.max(64, raw.length / 4));
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(raw);
        } catch (IOException ex) {
            throw new ReplayStorageException("Failed to compress replay payload", ex);
        }
        byte[] compressed = buffer.toByteArray();
        return new CompressedPayload(compressed, checksum(compressed), raw.length);
    }

    public byte[] decompress(byte[] compressed, String expectedChecksum) {
        String actualChecksum = checksum(compressed);
        if (expectedChecksum != null && !expectedChecksum.equals(actualChecksum)) {
            throw new ReplayStorageException(
                    "Replay payload checksum mismatch: expected "
                            + expectedChecksum
                            + " but was "
                            + actualChecksum,
                    null);
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return gzip.readAllBytes();
        } catch (IOException ex) {
            throw new ReplayStorageException("Failed to decompress replay payload", ex);
        }
    }

    public String checksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new ReplayStorageException("SHA-256 is unavailable in this runtime", ex);
        }
    }
}
