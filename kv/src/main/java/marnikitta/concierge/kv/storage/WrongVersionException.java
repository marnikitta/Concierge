package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class WrongVersionException extends ConciergeActionException {
  public WrongVersionException(String key, long version) {
    super("Wrong version " + version + " of entry with key " + key);
  }

  @Override
  public int code() {
    return 10;
  }
}
