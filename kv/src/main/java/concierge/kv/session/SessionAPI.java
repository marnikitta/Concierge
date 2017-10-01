package concierge.kv.session;

import concierge.kv.ConciergeAction;
import concierge.kv.storage.Storage;

import java.time.Instant;

public interface SessionAPI {
  final class CreateSession implements ConciergeAction {
    private final long sessionId;

    public CreateSession(long sessionId) {
      this.sessionId = sessionId;
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
    public Object doIt(Storage storage, SessionManager manager, Instant ts) throws SessionExistsException {
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
    public Object doIt(Storage storage, SessionManager manager, Instant ts) throws NoSuchSessionException, SessionExpiredException {
      if (manager.get(sessionId).isExpired(ts)) {
        throw new SessionExpiredException(sessionId);
      }
      manager.heartbeat(sessionId, ts);
      return null;
    }
  }
}
