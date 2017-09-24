package concierge.kv.session;

import concierge.kv.ConciergeAction;
import concierge.kv.storage.Storage;

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
    public Object doIt(Storage storage, SessionManager manager) throws SessionExistsException {
      return manager.create(sessionId);
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
    public Object doIt(Storage storage, SessionManager manager) throws NoSuchSessionException {
      manager.heartbeat(sessionId);
      return null;
    }
  }
}
