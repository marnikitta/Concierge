package concierge.kv.storage;

import java.util.HashMap;
import java.util.Map;

public final class Storage {
  private final Map<String, StorageEntry> storage = new HashMap<>();

  public StorageEntry createEphemeral(String key, byte[] payload, long sessionId) throws KeyAlreadyExistsException {
    if (contains(key)) {
      throw new KeyAlreadyExistsException(key);
    } else {
      final StorageEntry entry = new StorageEntry(key, payload, sessionId, true);
      storage.put(key, entry);
      return entry;
    }
  }

  public StorageEntry get(String key) throws NoSuchKeyException {
    final StorageEntry value = storage.get(key);
    if (value == null) {
      throw new NoSuchKeyException(key);
    } else {
      return storage.get(key).copy();
    }
  }

  public boolean contains(String key) {
    return storage.containsKey(key);
  }

  public StorageEntry create(String key, byte[] payload, long sessionId) throws KeyAlreadyExistsException {
    if (contains(key)) {
      throw new KeyAlreadyExistsException(key);
    } else {
      final StorageEntry entry = new StorageEntry(key, payload, sessionId, false);
      storage.put(key, entry);
      return entry;
    }
  }

  public StorageEntry update(String key, byte[] value, long expectedVersion, long sessionId) throws NoSuchKeyException, WrongSessionException, WrongVersionException {
    final StorageEntry entry = get(key);
    if (entry.ephemeral() && sessionId != entry.sessionId()) {
      throw new WrongSessionException(key, sessionId);
    } else if (expectedVersion != entry.version()) {
      throw new WrongVersionException(key, expectedVersion, entry.version());
    } else {
      final StorageEntry updated = entry.updated(value);
      storage.put(key, updated);
      return updated;
    }
  }
}
