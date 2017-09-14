package marnikitta.concierge.atomic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import marnikitta.concierge.paxos.DecreePresident;
import marnikitta.concierge.paxos.DecreePriest;
import marnikitta.concierge.paxos.PaxosAPI;
import marnikitta.concierge.paxos.PaxosMessage;
import marnikitta.leader.election.ElectorAPI;
import marnikitta.leader.election.OmegaElector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static marnikitta.concierge.atomic.AtomicBroadcastAPI.Broadcast;
import static marnikitta.concierge.atomic.AtomicBroadcastAPI.RegisterBroadcasts;
import static marnikitta.concierge.atomic.BroadcastMessages.ElectorIdentity;
import static marnikitta.concierge.atomic.BroadcastMessages.IdentifyElector;

public final class AtomicBroadcast extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final ActorRef omegaElector = null;

  private final Set<ActorRef> broadcasts = new HashSet<>();
  private final Map<ActorRef, ActorRef> electorToBroadcast = new HashMap<>();
  private final ActorRef subscriber;

  @Nullable
  private ActorRef currentLeader = null;
  private final TLongObjectMap<ActorRef> decrees = new TLongObjectHashMap<>();

  private long lastTxid;

  private AtomicBroadcast(ActorRef subscriber) {
    this.subscriber = subscriber;
  }

  public static Props props(ActorRef subscriber) {
    return Props.create(AtomicBroadcast.class, subscriber);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(IdentifyElector.class, i -> sender().tell(new ElectorIdentity(omegaElector), self()))
            .match(RegisterBroadcasts.class, registerBroadcasts -> {
              LOG.info("Broadcasts are registered {}", registerBroadcasts);
              broadcasts.addAll(registerBroadcasts.broadcasts);
              broadcasts.forEach(b -> b.tell(new IdentifyElector(), self()));
              getContext().become(identifyingElectors());
            })
            .build();
  }

  private Receive identifyingElectors() {
    return ReceiveBuilder.create()
            .match(IdentifyElector.class, i -> sender().tell(new ElectorIdentity(omegaElector), self()))
            .match(ElectorIdentity.class, identity -> {
              LOG.info("Received elector identity {}", identity);
              electorToBroadcast.put(identity.elector, sender());
              if (electorToBroadcast.values().containsAll(broadcasts)) {
                LOG.info("Received all identities");
                getContext().become(receivingBroadcasts());
              }
            })
            .build();
  }

  private Receive receivingBroadcasts() {
    return ReceiveBuilder.create()
            .match(IdentifyElector.class, i -> sender().tell(new ElectorIdentity(omegaElector), self()))
            .match(ElectorAPI.NewLeader.class, leader -> currentLeader = electorToBroadcast.get(leader.leader))
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

interface BroadcastMessages {
  class IdentifyElector {
  }

  class ElectorIdentity {
    public final ActorRef elector;

    public ElectorIdentity(ActorRef elector) {
      this.elector = elector;
    }

    @Override
    public String toString() {
      return "ElectorIdentity{" +
              "elector=" + elector +
              '}';
    }
  }
}
