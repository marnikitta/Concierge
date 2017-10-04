package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class NoSuchKeyException extends ConciergeActionException {
  public NoSuchKeyException(String key) {
    super("Entry with key " + key + " doesn't exist");
  }

  @Override
  public int code() {
    return 8;
  }
}
