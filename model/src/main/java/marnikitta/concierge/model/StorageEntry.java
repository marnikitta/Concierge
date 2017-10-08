package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public final class StorageEntry {
  private final String key;
  private final String value;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final long sessionId;
  private final boolean ephemeral;
  private final long version;

  public StorageEntry(String key,
                      String value,
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

  @JsonCreator
  private StorageEntry(@JsonProperty("key") String key,
                       @JsonProperty("value") String value,
                       @JsonProperty("created_at") Instant createdAt,
                       @JsonProperty("updated_at") Instant updatedAt,
                       @JsonProperty("session_id") long sessionId,
                       @JsonProperty("ephemeral") boolean ephemeral,
                       @JsonProperty("version") long version) {
    this.key = key;
    this.value = value;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.sessionId = sessionId;
    this.ephemeral = ephemeral;
    this.version = version;
  }

  @JsonProperty("key")
  public String key() {
    return key;
  }

  @JsonProperty("version")
  public long version() {
    return version;
  }

  @JsonProperty("value")
  public String value() {
    return value;
  }

  @JsonProperty("created_at")
  public Instant createdAt() {
    return createdAt;
  }

  @JsonProperty("updated_at")
  public Instant updateAt() {
    return updatedAt;
  }

  @JsonProperty("session_id")
  public long sessionId() {
    return sessionId;
  }

  @JsonProperty("ephemeral")
  public boolean ephemeral() {
    return ephemeral;
  }

  public StorageEntry updated(String value, Instant updatedAt) {
    return new StorageEntry(key, value, createdAt, updatedAt, sessionId, ephemeral, version + 1);
  }

  @Override
  public String toString() {
    return "StorageEntry{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", sessionId=" + sessionId +
            ", ephemeral=" + ephemeral +
            ", version=" + version +
            '}';
  }
}
