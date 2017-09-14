package marnikitta.failure.detector;

import akka.actor.AbstractActor;
import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Identify;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import marnikitta.concierge.common.Cluster;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;
import static marnikitta.failure.detector.DetectorMessages.CHECK_HEARTBEATS;
import static marnikitta.failure.detector.DetectorMessages.HEARTBEAT;
import static marnikitta.failure.detector.DetectorMessages.SEND_HEARTBEAT;

/**
 * Eventually strong failure-detector as described in
 * "Unreliable failure detectors for Reliable Distributed Systems", Chandra and Toueg
 */
public class EventuallyStrongDetector extends AbstractActor {
  public static final long HEARTBEAT_DELAY = MILLISECONDS.toNanos(200);

  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final Map<ActorRef, Long> detectors = new HashMap<>();
  private final NavigableSet<Long> ids;

  private final SortedSet<Long> suspected = new TreeSet<>();

  private final TLongLongMap lastBeat = new TLongLongHashMap();
  private final TLongLongMap currentDelay = new TLongLongHashMap();

  private final Cancellable checkHeartbeats = context().system().scheduler().schedule(
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          self(),
          CHECK_HEARTBEATS,
          context().dispatcher(),
          self()
  );

  private final Cancellable selfHeartbeat = context().system().scheduler().schedule(
          Duration.Zero(),
          Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
          self(),
          SEND_HEARTBEAT,
          context().dispatcher(),
          self()
  );

  private final Cluster cluster;

  private EventuallyStrongDetector(Cluster cluster) {
    this.cluster = cluster;
    this.ids = new TreeSet<>(cluster.paths.keySet());
    ids.forEach(id -> lastBeat.put(id, System.nanoTime() + SECONDS.toNanos(10)));
    ids.forEach(id -> currentDelay.put(id, HEARTBEAT_DELAY * 2));
  }

  public static Props props(Cluster cluster) {
    return Props.create(EventuallyStrongDetector.class, cluster);
  }

  @Override
  public void preStart() throws Exception {
    cluster.paths.forEach((id, path) -> {
      context().actorSelection(path).tell(new Identify(id), self());
    });

    super.preStart();
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
            .match(DetectorMessages.class, m -> m == HEARTBEAT, m -> onHeartbeat())
            .match(DetectorMessages.class, m -> m == SEND_HEARTBEAT, m -> sendHeartbeats())
            .match(DetectorMessages.class, m -> m == CHECK_HEARTBEATS, m -> checkHeartbeats())
            .match(ActorIdentity.class,
                    actorIdentity -> !actorIdentity.getActorRef().isPresent(),
                    actorIdentity -> {
                      context().system().scheduler().scheduleOnce(
                              Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
                              () -> context().actorSelection(cluster.paths.get(actorIdentity.correlationId()))
                                      .tell(new Identify(actorIdentity.correlationId()), self()),
                              context().dispatcher()
                      );
                    })
            .match(ActorIdentity.class,
                    actorIdentity -> actorIdentity.getActorRef().isPresent(),
                    actorIdentity -> {
                      detectors.put(actorIdentity.getRef(), (long) actorIdentity.correlationId());

                      if (ids.containsAll(cluster.paths.keySet())) {
                        LOG.info("All detector identities are received");
                      }
                    })
            .build();
  }

  private void sendHeartbeats() {
    for (ActorRef ref : detectors.keySet()) {
      ref.tell(HEARTBEAT, self());
    }
  }

  private void onHeartbeat() {
    if (detectors.keySet().contains(sender())) {
      final long id = detectors.get(sender());

      final long now = System.nanoTime();

      if (suspected.contains(id)) {
        context().parent().tell(new Restore(id), self());
        currentDelay.put(id, currentDelay.get(id) + HEARTBEAT_DELAY);
        LOG.info("Restored={}, currentDelay={}us", id, currentDelay.get(id));
        suspected.remove(id);
      }

      lastBeat.put(id, now);
    } else {
      LOG.warning("Detector doesn't contains sender {}", sender());
    }
  }

  private void checkHeartbeats() {
    final long now = System.nanoTime();
    //detectors are iterated in sorted order. If multiple detectors the would be detected in sorted order
    for (long id : ids) {
      if (now - lastBeat.get(id) > currentDelay.get(id) && !suspected.contains(id)) {
        LOG.info("Suspected {}", id);
        suspected.add(id);
        context().parent().tell(new Suspect(id), self());
      }
    }
  }
}

enum DetectorMessages {
  CHECK_HEARTBEATS,
  SEND_HEARTBEAT,
  HEARTBEAT
}
