package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ConciergeActionException extends Exception {
  private final int code;

  @JsonCreator
  public ConciergeActionException(@JsonProperty("message") String message,
                                  @JsonProperty("code") int code) {
    super(message);
    this.code = code;
  }

  @JsonProperty("message")
  public String message() {
    return getMessage();
  }

  @JsonProperty("code")
  public int code() {
    return code;
  }
}
