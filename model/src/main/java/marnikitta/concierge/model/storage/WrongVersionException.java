package marnikitta.concierge.model.storage;

import marnikitta.concierge.model.ConciergeException;

public class WrongVersionException extends ConciergeException {
  public WrongVersionException(String key, long version) {
    super("Wrong version " + version + " of entry with key " + key, 10);
  }
}
