package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class WrongSessionException extends ConciergeActionException {
  public WrongSessionException(String key) {
    super("Ephemeral entry with key " + key + " belongs to other session");
  }

  @Override
  public int code() {
    return 9;
  }
}
