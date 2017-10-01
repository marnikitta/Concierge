package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class WrongSessionException extends ConciergeActionException {
  private final String key;
  private final long sessionId;

  public WrongSessionException(String key, long sessionId) {
    this.key = key;
    this.sessionId = sessionId;
  }

  public String key() {
    return key;
  }

  public long sessionId() {
    return sessionId;
  }

  @Override
  public String toString() {
    return "WrongSessionException{" +
            "key='" + key + '\'' +
            ", id=" + sessionId +
            '}';
  }
}
