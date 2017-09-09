package marnikitta.failure.detector;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import scala.concurrent.duration.Duration;

import java.util.HashSet;
import java.util.Set;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static marnikitta.failure.detector.DetectorAPI.*;

public class EventuallyStrongDetector extends AbstractActor {
  private static final long MAX_DELAY = SECONDS.toNanos(5);
  private static final long HEARTBEAT_DELAY = SECONDS.toNanos(1);

  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final TObjectLongMap<ActorRef> lastBeat = new TObjectLongHashMap<>();
  private final TObjectLongMap<ActorRef> currentDelay = new TObjectLongHashMap<>();
  private final Set<ActorRef> cluster = new HashSet<>();

  private final ActorRef subscriber;

  private EventuallyStrongDetector(ActorRef subscriber) {
    this.subscriber = subscriber;

    this.context().system().scheduler().schedule(
            Duration.Zero(),
            Duration.create(HEARTBEAT_DELAY, NANOSECONDS),
            self(),
            new SendHeartbeat(),
            context().dispatcher(),
            self()
    );
  }

  public static Props props(ActorRef subscriber) {
    return Props.create(EventuallyStrongDetector.class, subscriber);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(Ping.class, this::onPing)
            .match(Pong.class, this::onPong)
            .match(AddParticipant.class, this::onAddParticipant)
            .match(CheckHeartbeats.class, this::onCheckHeartbeats)
            .build();
  }

  private void onPing(Ping p) {
    sender().tell(new Pong(), self());
  }

  private void onPong(final Pong pong) {
    if (cluster.contains(sender())) {
      lastBeat.put(sender(), System.nanoTime());
    }
  }

  private void onAddParticipant(AddParticipant request) {
    cluster.add(request.participant);
    currentDelay.put(request.participant, HEARTBEAT_DELAY);
  }

  private void onCheckHeartbeats(CheckHeartbeats check) {
    for (final ActorRef ref : cluster) {
      final long delay = System.nanoTime() - lastBeat.get(ref);
      if (delay > currentDelay.get(ref)) {
        subscriber.tell(new Suspect(ref), self());

        final long newDelay = delay * 2;
        if (newDelay > MAX_DELAY) {
          removeParticipant(ref);
        } else {
          currentDelay.put(ref, newDelay);
        }
      }
    }
  }

  private void removeParticipant(ActorRef ref) {
    cluster.remove(ref);
    currentDelay.remove(ref);
    lastBeat.remove(ref);
  }

  private static class Ping {
  }

  private static class Pong {
  }

  private static class CheckHeartbeats {
    final ActorRef actorRef;
    final long delay;

    CheckHeartbeats(ActorRef actorRef, long delay) {
      this.actorRef = actorRef;
      this.delay = delay;
    }

    @Override
    public String toString() {
      return "CheckHeartbeats{" + "actorRef=" + actorRef +
              ", delay=" + delay +
              '}';
    }
  }

  private static class SendHeartbeat {
  }
}
