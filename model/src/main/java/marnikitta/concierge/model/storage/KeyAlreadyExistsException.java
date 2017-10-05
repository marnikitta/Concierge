package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeActionException;

public class KeyAlreadyExistsException extends ConciergeActionException {
  public KeyAlreadyExistsException(String key) {
    super("Entry with key " + key + " already exists", 6);
  }
}
