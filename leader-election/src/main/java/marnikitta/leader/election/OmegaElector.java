package marnikitta.leader.election;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import marnikitta.failure.detector.DetectorAPI;
import marnikitta.failure.detector.EventuallyStrongDetector;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static marnikitta.leader.election.ElectorAPI.AddParticipant;
import static marnikitta.leader.election.ElectorAPI.NewLeader;

public class OmegaElector extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef failureDetector = context().actorOf(EventuallyStrongDetector.props(), "detector");

  private final SortedSet<ActorRef> electors = new TreeSet<>();
  private final Map<ActorRef, ActorRef> failureDetectorToElector = new HashMap<>();

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(AddParticipant.class, this::onAddParticipant)
            .match(DetectorIdentity.class, this::onDetectorIdentity)
            .match(DetectorAPI.Suspect.class, this::onSuspect)
            .match(DetectorAPI.Restore.class, this::onRestore)
            .build();
  }

  private void onSuspect(DetectorAPI.Suspect suspect) {
    final ActorRef elector = failureDetectorToElector.get(suspect.theSuspect);

    if (!electors.contains(elector)) {
      throw new IllegalStateException();
    }

    if (electors.first().equals(elector)) {
      context().parent().tell(new NewLeader(electors.first()), self());
    }

    electors.remove(elector);
  }

  private void onRestore(DetectorAPI.Restore restore) {
    final ActorRef elector = failureDetectorToElector.get(restore.theSuspect);

    if (electors.contains(elector)) {
      throw new IllegalStateException();
    }

    if (electors.isEmpty() || electors.first().compareTo(elector) > 0) {
      context().parent().tell(new NewLeader(elector), self());
    }

    electors.add(elector);
  }

  private void onAddParticipant(AddParticipant addParticipant) {
    addParticipant.participant.tell(new IdentifyDetector(), self());
  }

  private void onDetectorIdentity(DetectorIdentity identity) {
    failureDetectorToElector.put(identity.detector, sender());
    failureDetector.tell(new DetectorAPI.AddParticipant(identity.detector), self());
    onRestore(new DetectorAPI.Restore(identity.detector));
  }

  private static class IdentifyDetector {
  }

  private static class DetectorIdentity {

    public final ActorRef detector;

    public DetectorIdentity(ActorRef detector) {
      this.detector = detector;
    }

    @Override
    public String toString() {
      return "DetectorIdentity{" +
              "detector=" + detector +
              '}';
    }
  }
}
