package marnikitta.failure.detector;

import akka.actor.ActorRef;

public interface DetectorAPI {
  class Suspect {
    public final ActorRef theSuspect;

    public Suspect(ActorRef theSuspect) {
      this.theSuspect = theSuspect;
    }

    @Override
    public String toString() {
      return "Suspect{" + "theSuspect=" + theSuspect + '}';
    }
  }

  class Restore {
    public final ActorRef theSuspect;

    public Restore(ActorRef theSuspect) {
      this.theSuspect = theSuspect;
    }

    @Override
    public String toString() {
      return "Restore{" + "theSuspect=" + theSuspect + '}';
    }
  }

  class AddParticipant {
    public final ActorRef participant;

    public AddParticipant(ActorRef participant) {
      this.participant = participant;
    }

    @Override
    public String toString() {
      return "AddParticipant{" + "participant=" + participant + '}';
    }
  }
}
