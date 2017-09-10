package marnikitta.leader.election;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
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

  public static Props props() {
    return Props.create(OmegaElector.class);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(AddParticipant.class, this::onAddParticipant)
            .match(DetectorIdentity.class, this::onDetectorIdentity)
            .match(DetectorAPI.Suspect.class, this::onSuspect)
            .match(DetectorAPI.Restore.class, this::onRestore)
            .match(
                    IdentifyDetector.class,
                    identifyDetector ->
                            sender().tell(new DetectorIdentity(this.failureDetector), self())
            ).build();
  }

  private void onSuspect(DetectorAPI.Suspect suspect) {
    final ActorRef elector = failureDetectorToElector.get(suspect.theSuspect);

    if (!electors.contains(elector)) {
      throw new IllegalStateException();
    }

    if (electors.first().equals(elector)) {
      electors.remove(elector);
      if (electors.isEmpty()) {
        LOG.info("New leader elected {}", null);
        context().parent().tell(new NewLeader(null), self());
      } else {
        LOG.info("New leader elected {}", electors.first());
        context().parent().tell(new NewLeader(electors.first()), self());
      }
    } else {
      electors.remove(elector);
    }
  }

  private void onRestore(DetectorAPI.Restore restore) {
    final ActorRef elector = failureDetectorToElector.get(restore.theSuspect);

    if (electors.contains(elector)) {
      throw new IllegalStateException();
    }
    electors.add(elector);

    if (electors.first().equals(elector)) {
      LOG.info("New leader elected {}", elector);
      context().parent().tell(new NewLeader(elector), self());
    }

  }

  private void onAddParticipant(AddParticipant addParticipant) {
    LOG.info("Detector identity requested {}", addParticipant);
    addParticipant.participant.tell(new IdentifyDetector(), self());
  }

  private void onDetectorIdentity(DetectorIdentity identity) {
    LOG.info("Detector identity received {}", identity);
    failureDetectorToElector.put(identity.detector, sender());
    onRestore(new DetectorAPI.Restore(identity.detector));

    failureDetector.tell(new DetectorAPI.AddParticipant(identity.detector), self());
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
