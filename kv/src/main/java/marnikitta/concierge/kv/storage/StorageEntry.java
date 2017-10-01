package marnikitta.concierge.kv.storage;

import java.time.Instant;
import java.util.Arrays;

public final class StorageEntry {
  private final String key; private final byte[] value;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final long sessionId;
  private final boolean ephemeral;
  private final long version;

  public StorageEntry(String key,
                      byte[] value,
                      long sessionId,
                      boolean ephemeral,
                      Instant createdAt) {
    this.key = key;
    this.value = value;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
    this.sessionId = sessionId;
    this.ephemeral = ephemeral;
    this.version = 1;
  }

  private StorageEntry(String key,
                       byte[] value,
                       Instant createdAt,
                       Instant updatedAt,
                       long sessionId,
                       boolean ephemeral,
                       long version) {
    this.key = key;
    this.value = value;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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
    return updatedAt;
  }

  public long sessionId() {
    return sessionId;
  }

  public boolean ephemeral() {
    return ephemeral;
  }

  public StorageEntry updated(byte[] value, Instant updatedAt) {
    return new StorageEntry(key, value, createdAt, updatedAt, sessionId, ephemeral, version + 1);
  }

  @Override
  public String toString() {
    return "StorageEntry{" +
            "key='" + key + '\'' +
            ", value=" + Arrays.toString(value) +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", id=" + sessionId +
            ", ephemeral=" + ephemeral +
            ", version=" + version +
            '}';
  }
}
