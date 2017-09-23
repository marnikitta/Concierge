package concierge.kv.session;

import java.time.Duration;
import java.time.Instant;

public final class Session {
  private final long sessionId;
  private final Instant createdAt;
  private final Instant lastHeartbeatAt;
  private final Duration heartbeatDelay;

  public Session(long sessionId, Instant createdAt, Duration heartbeatDelay, Instant lastHeartbeatAt) {
    if (lastHeartbeatAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("Last heartbeat must be after session creation ts");
    }
    this.heartbeatDelay = heartbeatDelay;
    this.sessionId = sessionId;
    this.createdAt = createdAt;
    this.lastHeartbeatAt = lastHeartbeatAt;
  }

  public Duration heartbeatDelay() {
    return heartbeatDelay;
  }

  public long sessionId() {
    return sessionId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant lastHeartbeatAt() {
    return lastHeartbeatAt;
  }

  public boolean isExpired(Instant now) {
    return lastHeartbeatAt.plus(heartbeatDelay).isAfter(now);
  }

  public Session heartbeated(Instant lastHeartbeatAt) {
    if (lastHeartbeatAt.isBefore(this.lastHeartbeatAt)) {
      throw new IllegalArgumentException("Heartbeats should be monotonic");
    }

    return new Session(sessionId, createdAt, heartbeatDelay, lastHeartbeatAt);
  }

  @Override
  public String toString() {
    return "Session{" +
            "sessionId=" + sessionId +
            ", createdAt=" + createdAt +
            ", lastHeartbeatAt=" + lastHeartbeatAt +
            ", heartbeatDelay=" + heartbeatDelay +
            '}';
  }
}
