package concierge.kv.session;

import concierge.kv.ConciergeActionException;

public class NoSuchSessionException extends ConciergeActionException {
  private final long sessionId;

  public NoSuchSessionException(long sessionId) {
    this.sessionId = sessionId;
  }

  public long sessionId() {
    return sessionId;
  }

  @Override
  public String toString() {
    return "NoSuchSessionException{" +
            "id=" + sessionId +
            '}';
  }
}
