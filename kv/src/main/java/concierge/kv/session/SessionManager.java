package concierge.kv.session;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public final class SessionManager {
  private final TLongObjectMap<Session> sessions = new TLongObjectHashMap<>();
  private final Duration heartbeatDelay;

  public SessionManager() {
    this(Duration.ofSeconds(3));
  }

  public SessionManager(Duration heartbeatDelay) {
    this.heartbeatDelay = heartbeatDelay;
  }

  public boolean contains(long sessionId) {
    return sessions.containsKey(sessionId);
  }

  public Session create(long sessionId) throws SessionExistsException {
    if (sessions.containsKey(sessionId)) {
      throw new SessionExistsException(sessionId);
    } else {
      final Session session = new Session(sessionId, Instant.now(), heartbeatDelay, Instant.now());
      sessions.put(sessionId, session);
      return session;
    }
  }

  public Session get(long sessionId) throws NoSuchSessionException {
    if (sessions.containsKey(sessionId)) {
      return sessions.get(sessionId);
    } else {
      throw new NoSuchSessionException(sessionId);
    }
  }

  public void heartbeat(long sessionId) throws NoSuchSessionException {
    get(sessionId).heartbeated(Instant.now());
  }

  public boolean isExpired(long sessionId) throws NoSuchSessionException {
    return get(sessionId).isExpired(Instant.now());
  }

  public static void main(final String... args) {
  }

  public static class Lazy<V> {
    private V val;
    private volatile Supplier<V> supplier;

    public V get() {
      if (supplier != null) {
        synchronized (this) {
          if (val == null) {
            val = supplier.get();
            supplier = null;
          }
        }
      }
      return val;
    }
  }

}


