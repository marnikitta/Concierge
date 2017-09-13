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

import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;
import static marnikitta.leader.election.ElectorAPI.NewLeader;
import static marnikitta.leader.election.ElectorAPI.RegisterElectors;
import static marnikitta.leader.election.ElectorMessages.DetectorIdentity;
import static marnikitta.leader.election.ElectorMessages.IdentifyDetector;

public class OmegaElector extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef subscriber; private final ActorRef failureDetector = context().actorOf(EventuallyStrongDetector.props(), "detector");

  private final SortedSet<ActorRef> electors = new TreeSet<>();
  private final Map<ActorRef, ActorRef> failureDetectorToElector = new HashMap<>();

  private OmegaElector(ActorRef subscriber) {
    this.subscriber = subscriber;
  }

  public static Props props(ActorRef subscriber) {
    return Props.create(OmegaElector.class, subscriber);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(IdentifyDetector.class, identifyDetector ->
                    sender().tell(new DetectorIdentity(this.failureDetector), self()))
            .match(RegisterElectors.class, register -> {
              if (register.participant.isEmpty()) {
                throw new IllegalArgumentException("Participants shouldn't be empty");
              }
              electors.addAll(register.participant);
              electors.forEach(e -> e.tell(new IdentifyDetector(), self()));

              LOG.info("Electors registered");
              getContext().become(identifyingDetectors());
            })
            .build();
  }

  private Receive identifyingDetectors() {
    return ReceiveBuilder.create()
            .match(IdentifyDetector.class, identifyDetector ->
                    sender().tell(new DetectorIdentity(this.failureDetector), self()))
            .match(DetectorIdentity.class, identity -> {
              LOG.info("Received identity {}", identity);
              failureDetectorToElector.put(identity.detector, sender());

              if (failureDetectorToElector.values().containsAll(electors)) {
                LOG.info("All electors are registered");
                failureDetector.tell(new DetectorAPI.RegisterDetectors(failureDetectorToElector.keySet()), self());

                LOG.info("New leader is elected {}", electors.first());
                subscriber.tell(new NewLeader(electors.first()), sender());

                getContext().become(electorsRegistered());
              }
            })
            .build();
  }

  private Receive electorsRegistered() {
    return ReceiveBuilder.create()
            .match(IdentifyDetector.class, identifyDetector ->
                    sender().tell(new DetectorIdentity(this.failureDetector), self()))
            .match(Suspect.class, this::onSuspect)
            .match(Restore.class, this::onRestore)
            .build();
  }

  private void onSuspect(Suspect suspect) {
    final ActorRef elector = failureDetectorToElector.get(suspect.theSuspect);

    if (!electors.contains(elector)) {
      throw new IllegalStateException("Double suspecting, probably incorrect failure detector implementation");
    }

    if (electors.first().equals(elector)) {
      electors.remove(elector);
      if (electors.isEmpty()) {
        LOG.info("New leader elected {}", null);
        subscriber.tell(new NewLeader(null), self());
      } else {
        LOG.info("New leader elected {}", electors.first());
        subscriber.tell(new NewLeader(electors.first()), self());
      }
    } else {
      electors.remove(elector);
    }
  }

  private void onRestore(Restore restore) {
    final ActorRef elector = failureDetectorToElector.get(restore.theSuspect);

    if (electors.contains(elector)) {
      throw new IllegalStateException("Double restoring, probably incorrect failure detector implementation");
    }

    electors.add(elector);

    if (electors.first().equals(elector)) {
      LOG.info("New leader elected {}", elector);
      subscriber.tell(new NewLeader(elector), self());
    }
  }
}

interface ElectorMessages {
  class IdentifyDetector {
  }

  class DetectorIdentity {
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
