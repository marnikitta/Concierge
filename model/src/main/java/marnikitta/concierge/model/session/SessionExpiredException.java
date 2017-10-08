package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeException;

public class SessionExpiredException extends ConciergeException {
  public SessionExpiredException(long sessionId) {
    super("Session with id " + sessionId + " has expired", 5);
  }
}
