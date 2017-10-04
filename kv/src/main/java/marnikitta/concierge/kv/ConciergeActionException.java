package marnikitta.concierge.kv;

public class ConciergeActionException extends Exception {
  public ConciergeActionException() {
  }

  public ConciergeActionException(String message) {
    super(message);
  }

  public ConciergeActionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConciergeActionException(Throwable cause) {
    super(cause);
  }

  protected ConciergeActionException(String message,
                                     Throwable cause,
                                     boolean enableSuppression,
                                     boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public int code() {
    return 1;
  }
}
