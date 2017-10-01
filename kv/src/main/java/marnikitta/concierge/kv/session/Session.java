package marnikitta.concierge.kv.session;

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

  private Session(long id,
                 Instant createdAt,
                 Instant heartbeatedAt,
                 Duration heartbeatDelay) {
    this.id = id;
    this.createdAt = createdAt;
    this.heartbeatedAt = heartbeatedAt;
    this.heartbeatDelay = heartbeatDelay;
  }

  public Duration heartbeatDelay() {
    return heartbeatDelay;
  }

  public long id() {
    return id;
  }

  public Instant createdAt() {
    return createdAt;
  }

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
