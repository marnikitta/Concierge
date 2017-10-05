package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeActionException;

public class SessionExpiredException extends ConciergeActionException {
  public SessionExpiredException(long sessionId) {
    super("Session with id " + sessionId + " has expired", 5);
  }
}
