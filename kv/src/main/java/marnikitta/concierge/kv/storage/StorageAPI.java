package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeAction;
import marnikitta.concierge.kv.session.SessionAPI;
import marnikitta.concierge.kv.session.SessionManager;

import java.time.Instant;

public interface StorageAPI {
  final class Create implements ConciergeAction {
    private final String key;
    private final String value;
    private final long sessionId;
    private final boolean ephemeral;

    public Create(String key, String value, long sessionId, boolean ephemeral) {
      this.key = key;
      this.value = value;
      this.sessionId = sessionId;
      this.ephemeral = ephemeral;
    }

    public boolean ephemeral() {
      return ephemeral;
    }

    public String key() {
      return key;
    }

    public String value() {
      return value;
    }

    public long sessionId() {
      return sessionId;
    }

    @Override
    public Object apply(Storage storage, SessionManager manager, Instant ts) {
      new SessionAPI.Heartbeat(sessionId).apply(storage, manager, ts);

      if (ephemeral) {
        return storage.createEphemeral(key, value, sessionId, ts);
      } else {
        return storage.create(key, value, sessionId, ts);
      }
    }
  }

  final class Read implements ConciergeAction {
    private final String key;
    private final long sessionId;

    public Read(String key, long sessionId) {
      this.key = key;
      this.sessionId = sessionId;
    }

    public String key() {
      return key;
    }

    @Override
    public String toString() {
      return "Read{" +
              "key='" + key + '\'' +
              '}';
    }

    @Override
    public Object apply(Storage storage, SessionManager manager, Instant ts) {
      new SessionAPI.Heartbeat(sessionId).apply(storage, manager, ts);

      return storage.get(key, sessionId);
    }
  }

  final class Update implements ConciergeAction {
    public final String key;
    public final String value;
    public final long sessionId;
    public final long expectedVersion;

    public Update(String key, long expectedVersion, String value, long sessionId) {
      this.key = key;
      this.value = value;
      this.sessionId = sessionId;
      this.expectedVersion = expectedVersion;
    }

    @Override
    public Object apply(Storage storage, SessionManager manager, Instant ts) {
      new SessionAPI.Heartbeat(sessionId).apply(storage, manager, ts);

      return storage.update(key, value, expectedVersion, sessionId, ts);
    }
  }

  final class Delete implements ConciergeAction {
    public final String key;
    public final long sessionId;
    public final long expectedVersion;

    public Delete(String key, long expectedVersion, long sessionId) {
      this.key = key;
      this.sessionId = sessionId;
      this.expectedVersion = expectedVersion;
    }

    @Override
    public Object apply(Storage storage, SessionManager manager, Instant ts) {
      new SessionAPI.Heartbeat(sessionId).apply(storage, manager, ts);
      storage.delete(key, expectedVersion, sessionId, ts);
      return true;
    }
  }
}
