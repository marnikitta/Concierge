package concierge.kv.storage;

import concierge.kv.ConciergeAction;
import concierge.kv.ConciergeActionException;
import concierge.kv.session.SessionAPI;
import concierge.kv.session.SessionManager;

import java.time.Instant;

public interface StorageAPI {
  final class Create implements ConciergeAction {
    private final String key;
    private final byte[] payload;
    private final long sessionId;
    private final boolean ephemeral;

    public Create(String key, byte[] payload, long sessionId, boolean ephemeral) {
      this.key = key;
      this.payload = payload;
      this.sessionId = sessionId;
      this.ephemeral = ephemeral;
    }

    public boolean ephemeral() {
      return ephemeral;
    }

    public String key() {
      return key;
    }

    public byte[] payload() {
      return payload;
    }

    public long sessionId() {
      return sessionId;
    }

    @Override
    public Object doIt(Storage storage, SessionManager manager, Instant ts) throws ConciergeActionException {
      new SessionAPI.Heartbeat(sessionId).doIt(storage, manager, ts);

      if (ephemeral) {
        return storage.createEphemeral(key, payload, sessionId, ts);
      } else {
        return storage.create(key, payload, sessionId, ts);
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
    public Object doIt(Storage storage, SessionManager manager, Instant ts) throws ConciergeActionException {
      new SessionAPI.Heartbeat(sessionId).doIt(storage, manager, ts);

      return storage.get(key, sessionId);
    }
  }

  final class Update implements ConciergeAction {
    public final String key;
    public final byte[] value;
    public final long sessionId;
    public final long expectedVersion;

    public Update(String key, long expectedVersion, byte[] value, long sessionId) {
      this.key = key;
      this.value = value;
      this.sessionId = sessionId;
      this.expectedVersion = expectedVersion;
    }

    @Override
    public Object doIt(Storage storage, SessionManager manager, Instant ts) throws ConciergeActionException {
      new SessionAPI.Heartbeat(sessionId).doIt(storage, manager, ts);

      return storage.update(key, value, expectedVersion, sessionId, ts);
    }
  }
}
