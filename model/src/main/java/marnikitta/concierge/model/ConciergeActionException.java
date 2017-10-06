package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"cause", "stackTrack", "localizedMessage", "suppressed"})
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
