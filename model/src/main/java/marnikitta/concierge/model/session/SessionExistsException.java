package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeException;

public class SessionExistsException extends ConciergeException {
  public SessionExistsException(long sessionId) {
    super("Session with id " + sessionId + " already exists", 3);
  }
}
