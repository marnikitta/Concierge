package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class KeyAlreadyExistsException extends ConciergeActionException {
  private final String key;

  public KeyAlreadyExistsException(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override
  public String toString() {
    return "KeyAlreadyExistsException{" +
            "key='" + key + '\'' +
            '}';
  }
}
