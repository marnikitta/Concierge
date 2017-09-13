package marnikitta.concierge.atomic;

import akka.actor.ActorRef;

import java.util.HashSet;
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
}
