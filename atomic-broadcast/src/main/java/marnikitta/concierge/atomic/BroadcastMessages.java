package marnikitta.concierge.atomic;

import akka.actor.ActorRef;

public interface BroadcastMessages {
  class IdentifyElector {
  }

  class ElectorIdentity {
    public final ActorRef elector;

    public ElectorIdentity(ActorRef elector) {
      this.elector = elector;
    }

    @Override
    public String toString() {
      return "ElectorIdentity{" +
              "elector=" + elector +
              '}';
    }
  }
}
