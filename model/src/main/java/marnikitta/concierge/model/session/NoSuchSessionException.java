package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeException;

public class NoSuchSessionException extends ConciergeException {
  public NoSuchSessionException(long sessionId) {
    super("Session with id " + sessionId + "doesn't exists", 1);
  }
}
