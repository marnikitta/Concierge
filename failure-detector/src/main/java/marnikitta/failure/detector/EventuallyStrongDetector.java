package marnikitta.failure.detector;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import marnikitta.concierge.common.Cluster;
import scala.concurrent.duration.Duration;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;
import static marnikitta.failure.detector.DetectorMessages.*;
import static marnikitta.failure.detector.DetectorMessages.CHECK_HEARTBEATS;
import static marnikitta.failure.detector.DetectorMessages.SEND_HEARTBEAT;

/**
 * Eventually strong failure-detector as described in
 * "Unreliable failure detectors for Reliable Distributed Systems", Chandra and Toueg
 */
public class EventuallyStrongDetector extends AbstractActor {
  public static final long HEARTBEAT_DELAY = MILLISECONDS.toNanos(200);

  private final LoggingAdapter LOG = Logging.getLogger(this);
  private final long id;

  private final NavigableMap<Long, ActorSelection> detectors = new TreeMap<>();

  private final TLongSet suspected = new TLongHashSet();

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

  private EventuallyStrongDetector(long id, Cluster cluster) {
    this.id = id;
    cluster.paths.forEach((i, path) -> detectors.put(i, context().actorSelection(path)));

    cluster.paths.keySet().forEach(i -> lastBeat.put(i, System.nanoTime() + SECONDS.toNanos(10)));
    cluster.paths.keySet().forEach(i -> currentDelay.put(i, HEARTBEAT_DELAY * 2));
  }

  public static Props props(long id, Cluster cluster) {
    return Props.create(EventuallyStrongDetector.class, id, cluster);
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
            .match(Heartbeat.class, this::onHeartbeat)
            .match(DetectorMessages.class, m -> m == SEND_HEARTBEAT, m -> sendHeartbeats())
            .match(DetectorMessages.class, m -> m == CHECK_HEARTBEATS, m -> checkHeartbeats())
            .build();
  }

  private void sendHeartbeats() {
    for (ActorSelection ref : detectors.values()) {
      ref.tell(new Heartbeat(id), self());
    }
  }

  private void onHeartbeat(Heartbeat heartbeat) {
    final long now = System.nanoTime();

    if (suspected.contains(heartbeat.id)) {
      context().parent().tell(new Restore(heartbeat.id), self());
      currentDelay.put(heartbeat.id, currentDelay.get(heartbeat.id) + HEARTBEAT_DELAY);
      LOG.info("Restored={}, currentDelay={}us", id, currentDelay.get(heartbeat.id));
      suspected.remove(heartbeat.id);
    }

    lastBeat.put(heartbeat.id, now);
  }

  private void checkHeartbeats() {
    final long now = System.nanoTime();
    //detectors are iterated in sorted order. If multiple detectors the would be detected in sorted order
    for (long i : detectors.navigableKeySet()) {
      if (now - lastBeat.get(i) > currentDelay.get(i) && !suspected.contains(i)) {
        LOG.info("Suspected {}", i);
        suspected.add(i);
        context().parent().tell(new Suspect(i), self());
      }
    }
  }
}

enum DetectorMessages {
  CHECK_HEARTBEATS,
  SEND_HEARTBEAT;

  public static class Heartbeat {
    public final long id;

    public Heartbeat(long id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return "Heartbeat{" +
              "id=" + id +
              '}';
    }
  }
}
