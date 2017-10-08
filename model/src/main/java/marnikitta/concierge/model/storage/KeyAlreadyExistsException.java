package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeException;

public class KeyAlreadyExistsException extends ConciergeException {
  public KeyAlreadyExistsException(String key) {
    super("Entry with key " + key + " already exists", 6);
  }
}
