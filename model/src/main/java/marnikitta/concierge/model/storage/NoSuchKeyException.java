package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeActionException;

public class NoSuchKeyException extends ConciergeActionException {
  public NoSuchKeyException(String key) {
    super("Entry with key " + key + " doesn't exist", 8);
  }
}
