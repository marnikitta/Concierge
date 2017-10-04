package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.time.Instant;

public final class Session {
  private final long id;
  private final Instant createdAt;
  private final Instant heartbeatedAt;
  private final Duration heartbeatDelay;

  public Session(long id,
                 Instant createdAt,
                 Duration heartbeatDelay) {
    this.heartbeatDelay = heartbeatDelay;
    this.id = id;
    this.createdAt = createdAt;
    this.heartbeatedAt = createdAt;
  }

  @JsonCreator
  private Session(@JsonProperty("id") long id,
                  @JsonProperty("created_at") Instant createdAt,
                  @JsonProperty("heartbeated_at") Instant heartbeatedAt,
                  @JsonProperty("heartbeat_delay") Duration heartbeatDelay) {
    this.id = id;
    this.createdAt = createdAt;
    this.heartbeatedAt = heartbeatedAt;
    this.heartbeatDelay = heartbeatDelay;
  }

  @JsonProperty("id")
  public long id() {
    return id;
  }

  @JsonProperty("heartbeat_delay")
  public Duration heartbeatDelay() {
    return heartbeatDelay;
  }

  @JsonProperty("created_at")
  public Instant createdAt() {
    return createdAt;
  }

  @JsonProperty("heartbeated_at")
  public Instant heartbeatedAt() {
    return heartbeatedAt;
  }

  public boolean isExpired(Instant now) {
    return heartbeatedAt.plus(heartbeatDelay).isBefore(now);
  }

  public Session heartbeated(Instant heartbeatedAt) {
    if (heartbeatedAt.isBefore(this.heartbeatedAt)) {
      throw new IllegalArgumentException("Heartbeats should be monotonic");
    }

    return new Session(id, createdAt, heartbeatedAt, heartbeatDelay);
  }

  @Override
  public String toString() {
    return "Session{" +
            "id=" + id +
            ", createdAt=" + createdAt +
            ", heartbeatedAt=" + heartbeatedAt +
            ", heartbeatDelay=" + heartbeatDelay +
            '}';
  }
}
