package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeActionException;

public class WrongVersionException extends ConciergeActionException {
  public WrongVersionException(String key, long version) {
    super("Wrong version " + version + " of entry with key " + key, 10);
  }
}
