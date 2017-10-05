package marnikitta.concierge.kv.session;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import marnikitta.concierge.model.session.NoSuchSessionException;
import marnikitta.concierge.model.Session;
import marnikitta.concierge.model.session.SessionExistsException;

import java.time.Duration;
import java.time.Instant;

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

  public Session create(long sessionId, Instant createTs) throws SessionExistsException {
    if (sessions.containsKey(sessionId)) {
      throw new SessionExistsException(sessionId);
    } else {
      final Session session = new Session(sessionId, createTs, heartbeatDelay);
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

  public void heartbeat(long sessionId, Instant heartbeatTs) throws NoSuchSessionException {
    sessions.put(sessionId, get(sessionId).heartbeated(heartbeatTs));
  }
}


