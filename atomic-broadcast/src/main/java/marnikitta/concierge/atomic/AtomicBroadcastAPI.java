package marnikitta.concierge.atomic;

import java.util.Objects;

public interface AtomicBroadcastAPI {
  class Broadcast {
    private final Object value;

    public Broadcast(Object value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Broadcast{" +
              "value=" + value() +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Broadcast broadcast = (Broadcast) o;
      return Objects.equals(value(), broadcast.value());
    }

    @Override
    public int hashCode() {
      return Objects.hash(value());
    }

    public Object value() {
      return value;
    }
  }

  class Deliver {
    private final Object value;

    public Deliver(Object value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Deliver{" +
              "value=" + value() +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Deliver deliver = (Deliver) o;
      return Objects.equals(value(), deliver.value());
    }

    @Override
    public int hashCode() {
      return Objects.hash(value());
    }

    public Object value() {
      return value;
    }
  }
}
