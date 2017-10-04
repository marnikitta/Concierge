package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ConciergeFailure {
  private final String message;
  private final int code;

  @JsonCreator
  public ConciergeFailure(@JsonProperty("message") String message,
                          @JsonProperty("code") int code) {
    this.message = message;
    this.code = code;
  }

  @JsonProperty("message")
  public String message() {
    return message;
  }

  @JsonProperty("code")
  public int code() {
    return code;
  }
}
