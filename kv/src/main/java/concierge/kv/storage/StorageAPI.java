package concierge.kv.storage;

import concierge.kv.ConciergeAction;
import concierge.kv.ConciergeActionException;
import concierge.kv.session.SessionExpiredException;
import concierge.kv.session.SessionManager;

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
    public Object doIt(Storage storage, SessionManager manager) throws ConciergeActionException {
      if (manager.isExpired(sessionId)) {
        throw new SessionExpiredException(sessionId);
      }
      manager.heartbeat(sessionId);
      if (ephemeral) {
        return storage.createEphemeral(key, payload, sessionId);
      } else {
        return storage.create(key, payload, sessionId);
      }
    }
  }

  class Created {
    private final StorageEntry entry;

    public Created(StorageEntry entry) {
      this.entry = entry;
    }

    public StorageEntry entry() {
      return entry;
    }

    @Override
    public String toString() {
      return "Created{" +
              "entry=" + entry +
              '}';
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
    public Object doIt(Storage storage, SessionManager manager) throws ConciergeActionException {
      if (manager.isExpired(sessionId)) {
        throw new SessionExpiredException(sessionId);
      }
      manager.heartbeat(sessionId);
      return storage.get(key);
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
    public Object doIt(Storage storage, SessionManager manager) throws ConciergeActionException {
      if (manager.isExpired(sessionId)) {
        throw new SessionExpiredException(sessionId);
      }
      manager.heartbeat(sessionId);

      return new Updated(storage.update(key, value, expectedVersion, sessionId));
    }
  }

  final class Updated {
    private final StorageEntry entry;

    public Updated(StorageEntry entry) {
      this.entry = entry;
    }

    public StorageEntry entry() {
      return entry;
    }

    @Override
    public String toString() {
      return "Updated{" +
              "entry=" + entry +
              '}';
    }
  }
}
