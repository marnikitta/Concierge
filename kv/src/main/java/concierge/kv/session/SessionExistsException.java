package concierge.kv.session;

import concierge.kv.ConciergeActionException;

public class SessionExistsException extends ConciergeActionException {
  private final long sessionId;

  public SessionExistsException(long sessionId) {
    this.sessionId = sessionId;
  }

  public long sessionId() {
    return sessionId;
  }

  @Override
  public String toString() {
    return "SessionExistsException{" +
            "id=" + sessionId +
            '}';
  }
}
