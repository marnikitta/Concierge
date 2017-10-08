package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeException;

public class NoSuchKeyException extends ConciergeException {
  public NoSuchKeyException(String key) {
    super("Entry with key " + key + " doesn't exist", 8);
  }
}
