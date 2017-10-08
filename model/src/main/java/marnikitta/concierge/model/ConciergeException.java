package marnikitta.concierge.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties({"cause", "stackTrace", "localizedMessage", "suppressed"})
public class ConciergeException extends RuntimeException {
  private final int code;

  @JsonCreator
  public ConciergeException(@JsonProperty("message") String message,
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
