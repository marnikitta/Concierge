package marnikitta.concierge.kv.storage;

import marnikitta.concierge.kv.ConciergeActionException;

public class WrongVersionException extends ConciergeActionException {
  private final String key;
  private final long expectedVersion;
  private final long version;

  public WrongVersionException(String key, long expectedVersion, long version) {
    this.key = key;
    this.expectedVersion = expectedVersion;
    this.version = version;
  }

  public String key() {
    return key;
  }

  public long expectedVersion() {
    return expectedVersion;
  }

  public long version() {
    return version;
  }

  @Override
  public String toString() {
    return "WrongVersionException{" +
            "key='" + key + '\'' +
            ", expectedVersion=" + expectedVersion +
            ", version=" + version +
            '}';
  }
}
