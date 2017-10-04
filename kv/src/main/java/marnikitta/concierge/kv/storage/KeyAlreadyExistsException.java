package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class KeyAlreadyExistsException extends ConciergeActionException {
  public KeyAlreadyExistsException(String key) {
    super("Entry with key " + key + " already exists");
  }

  @Override
  public int code() {
    return 6;
  }
}
