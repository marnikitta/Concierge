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
import org.jetbrains.annotations.Nullable;
import scala.concurrent.duration.Duration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static marnikitta.failure.detector.DetectorAPI.RegisterDetectors;
import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;
import static marnikitta.failure.detector.DetectorMessages.CheckHeartbeats;
import static marnikitta.failure.detector.DetectorMessages.Heartbeat;
import static marnikitta.failure.detector.DetectorMessages.SendHeartbeat;

/**
 * Eventually strong failure-detector as described in
 * "Unreliable failure detectors for Reliable Distributed Systems", Chandra and Toueg
 */
public class EventuallyStrongDetector extends AbstractActor {
  public static final long HEARTBEAT_DELAY = MILLISECONDS.toNanos(500);

  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final SortedSet<ActorRef> detectors = new TreeSet<>();
  private final SortedSet<ActorRef> suspected = new TreeSet<>();

  private final TObjectLongMap<ActorRef> lastBeat = new TObjectLongHashMap<>();
  private final TObjectLongMap<ActorRef> currentDelay = new TObjectLongHashMap<>();

  @Nullable
  private Cancellable checkHeartbeats = null;

  @Nullable
  private Cancellable selfHeartbeat = null;

  public static Props props() {
    return Props.create(EventuallyStrongDetector.class);
  }

  @Override
  public void preRestart(Throwable reason, Optional<Object> message) throws Exception {
    if (selfHeartbeat != null) {
      selfHeartbeat.cancel();
    }

    if (checkHeartbeats != null) {
      checkHeartbeats.cancel();
    }

    super.preRestart(reason, message);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(RegisterDetectors.class, register -> {
              detectors.addAll(register.detectors);

              checkHeartbeats = context().system().scheduler().schedule(
                      Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
                      Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
                      self(),
                      new CheckHeartbeats(),
                      context().dispatcher(),
                      self()
              );

              selfHeartbeat = context().system().scheduler().schedule(
                      Duration.Zero(),
                      Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
                      self(),
                      new SendHeartbeat(),
                      context().dispatcher(),
                      self()
              );

              getContext().become(participantsRegistered());
            }).build();
  }

  private Receive participantsRegistered() {
    return ReceiveBuilder.create()
            .match(Heartbeat.class, this::onHeartbeat)
            .match(SendHeartbeat.class, h -> sendHeartbeats())
            .match(CheckHeartbeats.class, c -> checkHeartbeats())
            .build();
  }

  private void sendHeartbeats() {
    for (ActorRef ref : detectors) {
      ref.tell(new Heartbeat(), self());
    }
  }

  private void onHeartbeat(Heartbeat heartbeat) {
    if (detectors.contains(sender())) {
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

  private void checkHeartbeats() {
    final long now = System.nanoTime();
    //detectors are iterated in sorted order. If multiple detectors the would be detected in sorted order
    for (ActorRef ref : detectors) {
      if (now - lastBeat.get(ref) > currentDelay.get(ref) && !suspected.contains(ref)) {
        LOG.info("Suspected {}", ref);
        suspected.add(ref);
        context().parent().tell(new Suspect(ref), self());
      }
    }
  }
}

interface DetectorMessages {
  class CheckHeartbeats {
  }

  class SendHeartbeat {
  }

  class Heartbeat {
  }
}
