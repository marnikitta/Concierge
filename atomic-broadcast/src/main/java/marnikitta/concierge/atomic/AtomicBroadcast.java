package marnikitta.concierge.atomic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.paxos.DecreePresident;
import marnikitta.concierge.paxos.DecreePriest;
import marnikitta.concierge.paxos.PaxosAPI;
import marnikitta.concierge.paxos.PaxosMessage;
import marnikitta.leader.election.ElectorAPI;
import marnikitta.leader.election.OmegaElector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static marnikitta.concierge.atomic.AtomicBroadcastAPI.Broadcast;

public final class AtomicBroadcast extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef omegaElector;
  private final ActorRef subscriber;
  private final Cluster cluster;

  private final Map<ActorRef, Long> broadcasts = new HashMap<>();

  @Nullable
  private ActorRef currentLeader = null;
  private final TLongObjectMap<ActorRef> decrees = new TLongObjectHashMap<>();

  private long lastTxid;

  private AtomicBroadcast(long id, ActorRef subscriber, Cluster cluster) {
    this.subscriber = subscriber;
    this.cluster = cluster;

    this.omegaElector = context().actorOf(
            OmegaElector.props(self(), new Cluster(cluster.paths, "elector")),
            "elector"
    );
  }

  public static Props props(long id, ActorRef subscriber, Cluster cluster) {
    return Props.create(AtomicBroadcast.class, id, subscriber, cluster);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .build();
  }

  private Receive receivingBroadcasts() {
    return ReceiveBuilder.create()
            .match(Broadcast.class, this::onBroadcast)
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            .build();
  }

  private void onBroadcast(Broadcast<?> broadcast) {
    if (currentLeader == null) {
      LOG.warning("There is no leader yet, abandoning message {}", broadcast);
      unhandled(broadcast);
    } else if (self().equals(currentLeader)) {
      lastTxid++;
      //TODO: delete leaders on timeout
      final ActorRef lead = context().actorOf(DecreePresident.props(broadcasts, lastTxid));
      lead.tell(new PaxosAPI.Propose<>(broadcast.value, lastTxid), self());
    } else {
      LOG.info("Redirecting message to the current leader, message={}, leader={}", broadcast, currentLeader);
      currentLeader.tell(broadcast, sender());
    }
  }

  private long lastDeliveredTxid = -1;
  private final SortedMap<Long, Object> pending = new TreeMap<>();

  private void onDecide(PaxosAPI.Decide<?> decide) {
    if (decide.txid <= lastDeliveredTxid) {
      LOG.info("txid={} is already delivered", decide.txid);
    } else {
      LOG.info("Enqueued txid={}", decide.txid);
      pending.put(decide.txid, decide.value);
    }

    while (pending.firstKey() == lastDeliveredTxid + 1) {
      final long first = pending.firstKey();
      LOG.info("Delivering txid={}", first);
      subscriber.tell(new AtomicBroadcastAPI.Deliver<>(pending.get(first)), self());
      pending.remove(first);
      lastDeliveredTxid = first;
    }
  }

  private void onPaxosMessage(PaxosMessage paxosMessage) {
    if (decrees.containsKey(paxosMessage.txid())) {
      decrees.get(paxosMessage.txid()).tell(paxosMessage, sender());
    } else {
      LOG.info("Created priest for txid={}", paxosMessage.txid());
      final ActorRef priest = context().actorOf(DecreePriest.props(paxosMessage.txid(), self()));
      priest.tell(paxosMessage, sender());
    }
  }
}
