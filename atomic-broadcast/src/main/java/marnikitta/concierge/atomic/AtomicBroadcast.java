package marnikitta.concierge.atomic;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.*;
import static java.util.Comparator.*;
import static marnikitta.concierge.atomic.AtomicBroadcastAPI.Broadcast;
import static marnikitta.concierge.atomic.AtomicBroadcastMessages.*;

/**
 * <h3>Specification:</h3>
 * <ol>
 * <li>
 * <b>Validity:</b> if a correct participant broadcasts a message, then all correct participants will eventually deliver it.
 * </li>
 * <li>
 * <b>Uniform Agreement:</b> if one correct participant delivers a message, then all correct participants will eventually deliver that message.
 * </li>
 * <li>
 * <b>Uniform Integrity:</b> a message is delivered by each participant at most once, and only if it was previously broadcast.
 * </li>
 * <li>
 * <b>Uniform Total Order:</b> the messages are totally ordered in the mathematical sense;
 * that is, if any correct participant delivers message 1 first and message 2 second, then every other correct participant must deliver message 1 before message 2.
 * </li>
 * </ol>
 */

public final class AtomicBroadcast extends AbstractActorWithStash {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  //Common state
  private final long id;
  private final ActorRef subscriber;
  private final Cluster cluster;
  private final TLongObjectMap<ActorSelection> broadcasts;

  private long currentLeader = -1;

  private AtomicBroadcast(long id, ActorRef subscriber, Cluster cluster) {
    this.id = id;
    this.subscriber = subscriber;
    this.cluster = cluster;

    this.broadcasts = new TLongObjectHashMap<>();
    cluster.paths.forEach((i, p) -> broadcasts.put(i, context().actorSelection(p)));

    context().actorOf(
            OmegaElector.props(id, self(), new Cluster(cluster.paths, "elector")),
            "elector"
    );
  }

  public static Props props(long id, ActorRef subscriber, Cluster cluster) {
    return Props.create(AtomicBroadcast.class, id, subscriber, cluster);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .build();
  }

  private void onNewLeader(ElectorAPI.NewLeader newLeader) {
    if (newLeader.leader == this.id) {
      getContext().become(leader());


    } else if (currentLeader == this.id) {
      getContext().become(priest());
    }

    this.currentLeader = newLeader.leader;
  }

  private final Set<LastDecided> lastDecidedTxid = new HashSet<>();

  private Receive syncing() {
    return ReceiveBuilder.create()
            .match(LastDecided.class, lastSeen -> {
              lastDecidedTxid.add(lastSeen);

              if (lastDecidedTxid.size() >= broadcasts.size()) {
                final long maxLastSeen = max(lastDecidedTxid, comparingLong(l -> l.txid)).txid;
                for (long i = lastProposedTxid; i <= maxLastSeen; ++i) {
                  final ActorRef lead = context().actorOf(DecreePresident.props(cluster, lastProposedTxid));
                  lead.tell(new PaxosAPI.Propose<>(OLIVE_DAY, lastProposedTxid), self());
                }
              }
            })
            .matchAny(m -> stash())
            .build();
  }

  private Receive leader() {
    return ReceiveBuilder.create()
            .match(Broadcast.class, this::onLeaderBroadcast)
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .build();
  }

  private Receive priest() {
    return ReceiveBuilder.create()
            .match(Broadcast.class, b -> broadcasts.get(currentLeader).tell(b, sender()))
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .build();
  }

  private long lastProposedTxid = -1;

  private void onLeaderBroadcast(Broadcast<?> broadcast) {
    lastProposedTxid++;
    final ActorRef lead = context().actorOf(DecreePresident.props(cluster, lastProposedTxid));
    lead.tell(new PaxosAPI.Propose<>(broadcast.value, lastProposedTxid), self());
  }

  private long lastDeliveredTxid = -1;

  private final SortedMap<Long, Object> pendingDecisions = new TreeMap<>();

  private void onDecide(PaxosAPI.Decide<?> decide) {
    if (decide.txid <= lastDeliveredTxid) {
      LOG.info("txid={} is already delivered", decide.txid);
    } else {
      LOG.info("Enqueued txid={}", decide.txid);
      pendingDecisions.put(decide.txid, decide.value);
    }

    while (pendingDecisions.firstKey() == lastDeliveredTxid + 1) {
      final long first = pendingDecisions.firstKey();
      LOG.info("Delivering txid={}", first);
      subscriber.tell(new AtomicBroadcastAPI.Deliver<>(pendingDecisions.get(first)), self());
      pendingDecisions.remove(first);
      lastDeliveredTxid = first;
    }
  }

  private final TLongObjectMap<ActorRef> decrees = new TLongObjectHashMap<>();

  private void onPaxosMessage(PaxosMessage paxosMessage) {
    if (decrees.containsKey(paxosMessage.txid())) {
      decrees.get(paxosMessage.txid()).tell(paxosMessage, sender());
    } else {
      LOG.info("Creating priest for txid={}", paxosMessage.txid());
      final ActorRef priest = context().actorOf(DecreePriest.props(paxosMessage.txid(), self()));
      priest.tell(paxosMessage, sender());
    }
  }
}

enum AtomicBroadcastMessages {
  OLIVE_DAY;

  public static class TellMeLastSeenTxid {
  }

  public static class LastDecided {
    public final long txid;

    public LastDecided(long txid) {
      this.txid = txid;
    }

    @Override
    public String toString() {
      return "LastDecided{" +
              "txid=" + txid +
              '}';
    }
  }
}
