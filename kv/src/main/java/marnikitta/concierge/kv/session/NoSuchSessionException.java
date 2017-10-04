package marnikitta.concierge.kv.session;

import marnikitta.concierge.kv.ConciergeActionException;

public class NoSuchSessionException extends ConciergeActionException {
  public NoSuchSessionException(long sessionId) {
    super("Session with id " + sessionId + "doesn't exists");
  }

  @Override
  public int code() {
    return 2;
  }
}
