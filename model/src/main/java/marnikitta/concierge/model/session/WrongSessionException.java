package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeActionException;

public class WrongSessionException extends ConciergeActionException {
  public WrongSessionException(String key) {
    super("Ephemeral entry with key " + key + " belongs to other session", 9);
  }
}
