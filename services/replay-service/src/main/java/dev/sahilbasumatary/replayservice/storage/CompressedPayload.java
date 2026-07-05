package dev.sahilbasumatary.replayservice.storage;

/**
 * A gzip-compressed payload ready for object storage.
 *
 * @param data the compressed bytes
 * @param checksumSha256 hex-encoded SHA-256 of the compressed bytes, for integrity verification
 * @param uncompressedBytes size of the original payload before compression
 */
public record CompressedPayload(byte[] data, String checksumSha256, long uncompressedBytes) {

    public int sizeBytes() {
        return data.length;
    }
}
