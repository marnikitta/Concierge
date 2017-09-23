package concierge.kv.storage;

import java.time.Instant;
import java.util.Arrays;

public final class StorageEntry {
  private final String key;
  private final byte[] value;
  private final Instant createdAt;
  private final Instant lastUpdatedAt;
  private final long sessionId;
  private final boolean ephemeral;
  private final long version;

  public StorageEntry(String key,
                      byte[] value,
                      long sessionId,
                      boolean ephemeral) {
    final Instant now = Instant.now();
    this.key = key;
    this.value = value;
    this.createdAt = now;
    this.lastUpdatedAt = now;
    this.sessionId = sessionId;
    this.ephemeral = ephemeral;
    this.version = 1;
  }

  private StorageEntry(String key,
                       byte[] value,
                       Instant createdAt,
                       Instant lastUpdatedAt,
                       long sessionId,
                       boolean ephemeral,
                       long version) {
    this.key = key;
    this.value = value;
    this.createdAt = createdAt;
    this.lastUpdatedAt = lastUpdatedAt;
    this.sessionId = sessionId;
    this.ephemeral = ephemeral;
    this.version = version;
  }

  public long version() {
    return version;
  }

  public String key() {
    return key;
  }

  public byte[] value() {
    return value;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastUpdatedAt() {
    return lastUpdatedAt;
  }

  public long sessionId() {
    return sessionId;
  }

  public boolean ephemeral() {
    return ephemeral;
  }

  public StorageEntry updated(byte[] value) {
    return new StorageEntry(key, value, createdAt, Instant.now(), sessionId, ephemeral, version + 1);
  }

  public StorageEntry copy() {
    return new StorageEntry(key, value, createdAt, lastUpdatedAt, sessionId, ephemeral, version);
  }

  @Override
  public String toString() {
    return "StorageEntry{" +
            "key='" + key + '\'' +
            ", value=" + Arrays.toString(value) +
            ", createdAt=" + createdAt +
            ", lastUpdatedAt=" + lastUpdatedAt +
            ", sessionId=" + sessionId +
            ", ephemeral=" + ephemeral +
            ", version=" + version +
            '}';
  }
}
