package marnikitta.leader.election;

import akka.actor.ActorRef;

public interface ElectorAPI {
  class AddParticipant implements ElectorAPI {
    public final ActorRef participant;

    public AddParticipant(ActorRef participant) {
      this.participant = participant;
    }

    @Override
    public String toString() {
      return "AddParticipant{" +
              "participant=" + participant +
              '}';
    }
  }

  class NewLeader implements ElectorAPI {
    public final ActorRef leader;

    public NewLeader(ActorRef leader) {
      this.leader = leader;
    }

    @Override
    public String toString() {
      return "NewLeader{" + "leader=" + leader + '}';
    }
  }
}
