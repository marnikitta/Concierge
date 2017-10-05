package marnikitta.concierge.kv.storage;

import marnikitta.concierge.model.storage.KeyAlreadyExistsException;
import marnikitta.concierge.model.storage.NoSuchKeyException;
import marnikitta.concierge.model.StorageEntry;
import marnikitta.concierge.model.session.WrongSessionException;
import marnikitta.concierge.model.storage.WrongVersionException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class Storage {
  private final Map<String, StorageEntry> storage = new HashMap<>();

  public StorageEntry createEphemeral(String key,
                                      String payload,
                                      long sessionId,
                                      Instant ts) throws KeyAlreadyExistsException {
    if (contains(key)) {
      throw new KeyAlreadyExistsException(key);
    } else {
      final StorageEntry entry = new StorageEntry(key, payload, sessionId, true, ts);
      storage.put(key, entry);
      return entry;
    }
  }

  public void expireEphemerals(long sessionId) {
    storage.forEach((key, entry) -> {
      if (entry.sessionId() == sessionId && entry.ephemeral()) {
        storage.remove(key);
      }
    });
  }

  public StorageEntry get(String key, long sessionId) throws NoSuchKeyException, WrongSessionException {
    final StorageEntry value = storage.get(key);
    if (value == null) {
      throw new NoSuchKeyException(key);
    } else {
      final StorageEntry entry = storage.get(key);
      if (entry.ephemeral() && entry.sessionId() != sessionId) {
        throw new WrongSessionException(key);
      } else {
        return entry;
      }
    }
  }

  public boolean contains(String key) {
    return storage.containsKey(key);
  }

  public StorageEntry create(String key,
                             String payload,
                             long sessionId,
                             Instant ts) throws KeyAlreadyExistsException {
    if (contains(key)) {
      throw new KeyAlreadyExistsException(key);
    } else {
      final StorageEntry entry = new StorageEntry(key, payload, sessionId, false, ts);
      storage.put(key, entry);
      return entry;
    }
  }

  public StorageEntry update(String key,
                             String value,
                             long expectedVersion,
                             long sessionId,
                             Instant ts) throws NoSuchKeyException, WrongSessionException, WrongVersionException {
    final StorageEntry entry = get(key, sessionId);
    if (expectedVersion != entry.version()) {
      throw new WrongVersionException(key, expectedVersion);
    } else {
      final StorageEntry updated = entry.updated(value, ts);
      storage.put(key, updated);
      return updated;
    }
  }
}
