package marnikitta.concierge.atomic;

import akka.actor.ActorRef;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public interface AtomicBroadcastAPI {
  class RegisterBroadcasts {
    public final Set<ActorRef> broadcasts;

    public RegisterBroadcasts(Set<ActorRef> broadcasts) {
      this.broadcasts = new HashSet<>(broadcasts);
    }

    @Override
    public String toString() {
      return "RegisterBroadcasts{" +
              "broadcasts=" + broadcasts +
              '}';
    }
  }

  class Broadcast<T> {
    public final T value;

    public Broadcast(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Broadcast{" +
              "value=" + value +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Broadcast<?> broadcast = (Broadcast<?>) o;
      return Objects.equals(value, broadcast.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }

  class Deliver<T> {
    public final T value;

    public Deliver(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Deliver{" +
              "value=" + value +
              '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Deliver<?> deliver = (Deliver<?>) o;
      return Objects.equals(value, deliver.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}
