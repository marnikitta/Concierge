package marnikitta.concierge.model.session;

import marnikitta.concierge.model.ConciergeException;

public class WrongSessionException extends ConciergeException {
  public WrongSessionException(String key) {
    super("Ephemeral entry with key " + key + " belongs to other session", 9);
  }
}
