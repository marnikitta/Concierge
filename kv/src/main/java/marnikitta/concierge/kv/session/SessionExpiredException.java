package marnikitta.concierge.kv.session;

import marnikitta.concierge.kv.ConciergeActionException;

public class SessionExpiredException extends ConciergeActionException {
  private final long sessionId;

  public SessionExpiredException(long sessionId) {
    this.sessionId = sessionId;
  }

  public long sessionId() {
    return sessionId;
  }

  @Override
  public String toString() {
    return "SessionExpiredException{" +
            "id=" + sessionId +
            '}';
  }
}
