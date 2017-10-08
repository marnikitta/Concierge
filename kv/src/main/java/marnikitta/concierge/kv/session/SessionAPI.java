package marnikitta.concierge.kv.session;

import marnikitta.concierge.kv.ConciergeAction;
import marnikitta.concierge.kv.storage.Storage;
import marnikitta.concierge.model.session.SessionExpiredException;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public interface SessionAPI {
  final class CreateSession implements ConciergeAction {
    private final long sessionId;

    public CreateSession() {
      this.sessionId = Math.abs(ThreadLocalRandom.current().nextLong());
    }

    @Override
    public String toString() {
      return "CreateSession{" +
              "id=" + sessionId() +
              '}';
    }

    public long sessionId() {
      return sessionId;
    }

    @Override
    public Object doIt(Storage storage, SessionManager manager, Instant ts) {
      return manager.create(sessionId, ts);
    }
  }

  final class Heartbeat implements ConciergeAction {
    private final long sessionId;

    public Heartbeat(long sessionId) {
      this.sessionId = sessionId;
    }

    public long sessionId() {
      return sessionId;
    }

    @Override
    public String toString() {
      return "Heartbeat{" +
              "id=" + sessionId() +
              '}';
    }

    @Override
    public Object doIt(Storage storage, SessionManager manager, Instant ts) {
      if (manager.get(sessionId).isExpired(ts)) {
        throw new SessionExpiredException(sessionId);
      }
      manager.heartbeat(sessionId, ts);
      return manager.get(sessionId);
    }
  }
}
