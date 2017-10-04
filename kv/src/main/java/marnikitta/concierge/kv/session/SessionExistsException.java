package marnikitta.concierge.kv.session;

import marnikitta.concierge.kv.ConciergeActionException;

public class SessionExistsException extends ConciergeActionException {
  public SessionExistsException(long sessionId) {
    super("Session with id " + sessionId + " already exists");
  }

  @Override
  public int code() {
    return 3;
  }
}
