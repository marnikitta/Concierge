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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NavigableMap;
import java.util.TreeMap;

import static marnikitta.concierge.atomic.AtomicBroadcastAPI.Broadcast;
import static marnikitta.concierge.atomic.AtomicBroadcastMessages.OLIVE_DAY;

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
 * <b>Uniform Integrity:</b> a message is delivered by each participant at most once, and only if it was previously broadcasting.
 * </li>
 * <li>
 * <b>Uniform Total Order:</b> the messages are totally ordered in the mathematical sense;
 * that is, if any correct participant delivers message 1 first and message 2 second, then every other correct participant must deliver message 1 before message 2.
 * </li>
 * </ol>
 */

public final class AtomicBroadcast extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef subscriber;
  private final Cluster cluster;

  private long lastDeliveredTxid = -1;

  private final Deque<Object> broadcastsDeque = new ArrayDeque<>();

  @Nullable
  private Object inFlightDecree = null;

  private long inFlightTxid = -1;

  private final NavigableMap<Long, Object> learnedDecrees = new TreeMap<>();

  private AtomicBroadcast(ActorRef subscriber, Cluster cluster) {
    learnedDecrees.put(-1L, OLIVE_DAY);
    this.subscriber = subscriber;
    this.cluster = cluster;
  }

  public static Props props(ActorRef subscriber, Cluster cluster) {
    return Props.create(AtomicBroadcast.class, subscriber, cluster);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            .match(Broadcast.class, this::onBroadcast)
            .build();
  }

  private void onBroadcast(Broadcast broadcast) {
    broadcastsDeque.offer(broadcast.value());
    tryBroadcastFromQueue();
  }

  private void tryBroadcastFromQueue() {
    if (lastDeliveredTxid != learnedDecrees.lastKey()) {
      fillGaps();
    }

    if (!broadcastsDeque.isEmpty()) {
      if (lastDeliveredTxid == learnedDecrees.lastKey() && inFlightDecree == null) {
        final long txid = learnedDecrees.lastKey() + 1;

        final Object decree = broadcastsDeque.poll();
        inFlightDecree = decree;
        inFlightTxid = txid;
        LOG.info("Broadcasting from queue decree={} txid={}", decree, txid);
        propose(decree, txid);
      }
    }
  }

  private void propose(Object value, long txid) {
    final String suffix = "president" + txid;
    if (!getContext().findChild(suffix).isPresent()) {
      final ActorRef lead = context().actorOf(DecreePresident.props(cluster, txid), suffix);
      lead.tell(new PaxosAPI.Propose(value, txid), self());
    } else {
      LOG.warning("There is a president for txid={} exists", txid);
    }
  }

  private void onDecide(PaxosAPI.Decide decide) {
    if (decide.txid() <= lastDeliveredTxid) {
      LOG.error("fromTxid={} is already delivered", decide.txid());
    } else {
      LOG.info("Learned decree for txid={}", decide.txid());
      learnedDecrees.put(decide.txid(), decide.value());
      tryDeliver();
    }

    if (inFlightDecree != null && decide.txid() == inFlightTxid) {
      if (inFlightDecree.equals(decide.value())) {
        LOG.info("Decision for awaited decree is complete txid={}", decide.txid());
      } else {
        LOG.warning("Somebody broadcasts before me, retry");
        broadcastsDeque.offerFirst(inFlightDecree);
      }

      inFlightDecree = null;
      inFlightTxid = -1;
    }

    tryBroadcastFromQueue();
  }

  private void tryDeliver() {
    while (learnedDecrees.containsKey(lastDeliveredTxid + 1)) {
      lastDeliveredTxid++;

      final Object decree = learnedDecrees.get(lastDeliveredTxid);
      if (decree != OLIVE_DAY) {
        LOG.info("Delivering fromTxid={}", lastDeliveredTxid);
        subscriber.tell(new AtomicBroadcastAPI.Deliver(decree), self());
      }
    }
  }

  private void fillGaps() {
    LOG.info("Filling gaps in ledger");
    for (long txid = lastDeliveredTxid + 1; txid <= learnedDecrees.lastKey(); txid++) {
      if (!learnedDecrees.containsKey(txid)) {
        propose(OLIVE_DAY, txid);
      }
    }
  }

  //Route paxos messages to the appropriate instance

  private final TLongObjectMap<ActorRef> priests = new TLongObjectHashMap<>();

  private void onPaxosMessage(PaxosMessage paxosMessage) {
    if (priests.containsKey(paxosMessage.txid())) {
      priests.get(paxosMessage.txid()).tell(paxosMessage, sender());
    } else {
      LOG.info("Creating priest for fromTxid={}", paxosMessage.txid());
      final ActorRef priest = context().actorOf(DecreePriest.props(paxosMessage.txid(), self()), "priest" + paxosMessage.txid());
      priests.put(paxosMessage.txid(), priest);
      priest.tell(paxosMessage, sender());
    }
  }
}

enum AtomicBroadcastMessages {
  OLIVE_DAY
}
