package marnikitta.leader.election;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import marnikitta.concierge.common.Cluster;
import marnikitta.failure.detector.EventuallyStrongDetector;

import java.util.SortedSet;
import java.util.TreeSet;

import static marnikitta.failure.detector.DetectorAPI.Restore;
import static marnikitta.failure.detector.DetectorAPI.Suspect;
import static marnikitta.leader.election.ElectorAPI.NewLeader;

public final class OmegaElector extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef subscriber;

  private final SortedSet<Long> aliveElectors;

  private OmegaElector(long id, ActorRef subscriber, Cluster cluster) {
    this.aliveElectors = new TreeSet<>(cluster.paths().keySet());
    this.subscriber = subscriber;
    context().actorOf(
            EventuallyStrongDetector.props(id, new Cluster(cluster.paths(), "detector")),
            "detector"
    );
  }

  public static Props props(long id, ActorRef subscriber, Cluster cluster) {
    return Props.create(OmegaElector.class, id, subscriber, cluster);
  }

  @Override
  public void preStart() throws Exception {
    LOG.info("New leader elected by the default: {}", aliveElectors.first());
    subscriber.tell(new NewLeader(aliveElectors.first()), self());
    super.preStart();
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(Suspect.class, this::onSuspect)
            .match(Restore.class, this::onRestore)
            .build();
  }

  private void onSuspect(Suspect suspect) {
    if (!aliveElectors.contains(suspect.theSuspect)) {
      throw new IllegalStateException("Double suspecting, probably incorrect failure detector implementation");
    }

    if (aliveElectors.first().equals(suspect.theSuspect)) {
      aliveElectors.remove(suspect.theSuspect);
      if (aliveElectors.isEmpty()) {
        LOG.warning("There are no electors alive...");
      } else {
        LOG.info("New leader elected by the previous suspect: {}", aliveElectors.first());
        subscriber.tell(new NewLeader(aliveElectors.first()), self());
      }
    } else {
      aliveElectors.remove(suspect.theSuspect);
    }
  }

  private void onRestore(Restore restore) {
    if (aliveElectors.contains(restore.theSuspect)) {
      throw new IllegalStateException("Double restoring, probably incorrect failure detector implementation");
    }

    aliveElectors.add(restore.theSuspect);

    if (aliveElectors.first().equals(restore.theSuspect)) {
      LOG.info("New leader elected by the restoring: {}", restore.theSuspect);
      subscriber.tell(new NewLeader(restore.theSuspect), self());
    }
  }
}
