package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.session.SessionManager;
import marnikitta.concierge.model.StorageEntry;
import marnikitta.concierge.model.session.WrongSessionException;
import marnikitta.concierge.model.storage.KeyAlreadyExistsException;
import marnikitta.concierge.model.storage.NoSuchKeyException;
import marnikitta.concierge.model.storage.WrongVersionException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class Storage {
  private final Map<String, StorageEntry> storage = new HashMap<>();

  public StorageEntry create(String key,
                             String payload,
                             long sessionId,
                             Instant ts,
                             boolean ephemeral) {
    if (contains(key)) {
      throw new KeyAlreadyExistsException(key);
    } else {
      final StorageEntry entry = new StorageEntry(key, payload, sessionId, ephemeral, ts);
      storage.put(key, entry);
      return entry;
    }
  }

  public StorageEntry get(String key, SessionManager manager, Instant now) {
    final StorageEntry value = storage.get(key);
    if (value == null) {
      throw new NoSuchKeyException(key);
    } else {
      if (value.ephemeral() && manager.get(value.sessionId()).isExpired(now)) {
        storage.remove(key);
        throw new NoSuchKeyException(key);
      } else {
        return storage.get(key);
      }
    }
  }

  public boolean contains(String key) {
    return storage.containsKey(key);
  }

  public StorageEntry update(String key,
                             String value,
                             long expectedVersion,
                             long sessionId,
                             SessionManager manager,
                             Instant ts) {
    final StorageEntry entry = get(key, manager, ts);

    if (entry.sessionId() != sessionId) {
      throw new WrongSessionException(key);
    } else if (entry.version() != expectedVersion) {
      throw new WrongVersionException(key, expectedVersion);
    } else {
      final StorageEntry updated = entry.updated(value, ts);
      storage.put(key, updated);
      return updated;
    }
  }

  public void delete(String key,
                     long expectedVersion,
                     long sessionId,
                     SessionManager manager,
                     Instant ts) {
    final StorageEntry entry = get(key, manager, ts);
    if (entry.sessionId() != sessionId) {
      throw new WrongSessionException(key);
    } else if (entry.version() != expectedVersion) {
      throw new WrongVersionException(key, expectedVersion);
    } else {
      storage.remove(key);
    }
  }
}
