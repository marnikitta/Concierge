package marnikitta.concierge.kv.session;

import marnikitta.concierge.kv.ConciergeActionException;

public class SessionExpiredException extends ConciergeActionException {
  public SessionExpiredException(long sessionId) {
    super("Session with id " + sessionId + " has expired");
  }

  @Override
  public int code() {
    return 5;
  }
}
