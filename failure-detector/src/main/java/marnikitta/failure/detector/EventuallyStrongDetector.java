package marnikitta.failure.detector;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import scala.concurrent.duration.Duration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static marnikitta.failure.detector.DetectorAPI.AddParticipant;
import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;

/**
 * Eventually strong failure-detector as described in
 * "Unreliable failure detectors for Reliable Distributed Systems", Chandra and Toueg
 */
public class EventuallyStrongDetector extends AbstractActor {
  public static final long HEARTBEAT_DELAY = MILLISECONDS.toNanos(500);

private final LoggingAdapter LOG = Logging.getLogger(this);

  private final TObjectLongMap<ActorRef> lastBeat = new TObjectLongHashMap<>();
  private final TObjectLongMap<ActorRef> currentDelay = new TObjectLongHashMap<>();
  private final Set<ActorRef> cluster = new HashSet<>();

  private final Set<ActorRef> suspected = new HashSet<>();

  private final Cancellable checkHeartbeats = context().system().scheduler().schedule(
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          self(),
          new CheckHeartbeats(),
          context().dispatcher(),
          self()
  );

  private final Cancellable selfHeartbeat = context().system().scheduler().schedule(
          Duration.Zero(),
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          self(),
          new SendHeartbeat(),
          context().dispatcher(),
          self()
  );

  public static Props props() {
    return Props.create(EventuallyStrongDetector.class);
  }

  @Override
  public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
    selfHeartbeat.cancel();
    checkHeartbeats.cancel();

    super.preRestart(reason, message);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(Heartbeat.class, this::onHeartbeat)
            .match(SendHeartbeat.class, this::onSendHeartbeat)
            .match(AddParticipant.class, this::onAddParticipant)
            .match(CheckHeartbeats.class, this::onCheckHeartbeats)
            .build();
  }

  private void onSendHeartbeat(SendHeartbeat sendHeartbeat) {
    for (ActorRef ref : cluster) {
      ref.tell(new Heartbeat(), self());
    }
  }

  private void onHeartbeat(Heartbeat heartbeat) {
    if (cluster.contains(sender())) {
      final long now = System.nanoTime();
      final ActorRef heartbeater = sender();

      if (suspected.contains(heartbeater)) {
        context().parent().tell(new Restore(heartbeater), self());
        currentDelay.put(heartbeater, currentDelay.get(heartbeater) + HEARTBEAT_DELAY);
        LOG.info("Restored={}, currentDelay={}us", heartbeater, currentDelay.get(heartbeater));
        suspected.remove(heartbeater);
      }

      lastBeat.put(heartbeater, now);
    } else {
      unhandled(heartbeat);
    }
  }

  private void onCheckHeartbeats(CheckHeartbeats check) {
    final long now = System.nanoTime();
    for (ActorRef ref : cluster) {
      if (now - lastBeat.get(ref) > currentDelay.get(ref) && !suspected.contains(ref)) {
        LOG.info("Suspected {}", ref);
        suspected.add(ref);
        context().parent().tell(new Suspect(ref), self());
      }
    }
  }

  private void onAddParticipant(AddParticipant request) {
    LOG.info("Participant add {}", request.participant);
    cluster.add(request.participant);
    currentDelay.put(request.participant, HEARTBEAT_DELAY  + HEARTBEAT_DELAY);
    lastBeat.put(request.participant, System.nanoTime() + SECONDS.toNanos(10));
  }

  private static class CheckHeartbeats {
  }

  private static class SendHeartbeat {
  }

  private static class Heartbeat {
  }
}
