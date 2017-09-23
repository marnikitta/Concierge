package concierge.kv.storage;

import concierge.kv.ConciergeActionException;

public class NoSuchKeyException extends ConciergeActionException {
  private final String key;

  public NoSuchKeyException(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  @Override
  public String toString() {
    return "NoSuchKeyException{" +
            "key='" + key + '\'' +
            '}';
  }
}
