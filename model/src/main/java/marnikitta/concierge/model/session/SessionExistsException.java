package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeActionException;

public class SessionExistsException extends ConciergeActionException {
  public SessionExistsException(long sessionId) {
    super("Session with id " + sessionId + " already exists", 3);
  }
}
