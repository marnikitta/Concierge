package marnikitta.failure.detector;

import akka.actor.ActorRef;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

  class RegisterDetectors {
    public final Set<ActorRef> detectors;

    public RegisterDetectors(Collection<ActorRef> detectors) {
      this.detectors = new HashSet<>(detectors);
    }

    @Override
    public String toString() {
      return "RegisterDetectors{" +
              "detectors=" + detectors +
              '}';
    }
  }
}
