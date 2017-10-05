package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeActionException;

public class NoSuchSessionException extends ConciergeActionException {
  public NoSuchSessionException(long sessionId) {
    super("Session with id " + sessionId + "doesn't exists", 1);
  }
}
