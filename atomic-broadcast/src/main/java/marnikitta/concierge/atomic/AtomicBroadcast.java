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
import scala.concurrent.duration.Duration;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static marnikitta.concierge.atomic.AtomicBroadcastAPI.Broadcast;
import static marnikitta.concierge.atomic.AtomicBroadcastMessages.LearnedDecrees;
import static marnikitta.concierge.atomic.AtomicBroadcastMessages.OLIVE_DAY;
import static marnikitta.concierge.atomic.AtomicBroadcastMessages.TellMeYourLedger;

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
 * <p>
 * <h3>Notation</h3>
 * <b>Decree</b> - consensus instance, has unique txid
 * <b>Decree</b> - value of the consensus instance
 * <b>President</b> - proposer of decree
 * <b>Priest</b> - learner of the decree
 * <b>Txid</b> - id of the decree
 */

public final class AtomicBroadcast extends AbstractActorWithStash {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  //Common state
  private final long id;
  private final ActorRef subscriber;
  private final Cluster cluster;
  private final TLongObjectMap<ActorSelection> broadcasts;

  private final NavigableMap<Long, Object> learnedDecrees = new TreeMap<>();

  {
    learnedDecrees.put(-1L, OLIVE_DAY);
  }

  private long lastProposedTxid = -1;
  private long lastDeliveredTxid = -1;

  private AtomicBroadcast(long id, ActorRef subscriber, Cluster cluster) {
    this.id = id;
    this.subscriber = subscriber;
    this.cluster = cluster;

    this.broadcasts = new TLongObjectHashMap<>();
    cluster.paths.forEach((i, p) -> broadcasts.put(i, context().actorSelection(p)));
  }

  public static Props props(long id, ActorRef subscriber, Cluster cluster) {
    return Props.create(AtomicBroadcast.class, id, subscriber, cluster);
  }

  @Override
  public void preStart() throws Exception {
    //Restore ledger and deliver all messages
    super.preStart();
  }

  /**
   * Default state of the actor
   */
  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .matchAny(m -> stash())
            .build();
  }

  /**
   * Synchronization state of the actor. Happens after self leader election.
   * <p>
   * Possible transitions:
   * <ol>
   * <li>Sync -> leader, on sync completion</li>
   * <li>Sync -> priest, on NewLeader</li>
   * <li>Sync -> waiting, on "You are not my leader" messages</li>
   * </ol>
   */
  private Receive syncing() {
    return ReceiveBuilder.create()
            //common
            .match(TellMeYourLedger.class, this::tellMyLedger)
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, decide -> {
              onDecide(decide);
              if (lastDeliveredTxid == lastProposedTxid) {
                lead();
              }
            })
            //syncing specific
            .match(String.class, m -> m.equals("Resync"), m -> sync())
            .match(LearnedDecrees.class, decrees -> {
              LOG.info("Got learned decisions from {}", sender());
              this.learnedDecrees.putAll(decrees.ledger);
              receivedDecrees++;

              if (receivedDecrees >= broadcasts.size() / 2) {
                tryDeliver();
                for (long txid = lastDeliveredTxid + 1; txid <= learnedDecrees.lastKey(); ++txid) {
                  if (!learnedDecrees.containsKey(txid)) {
                    final ActorRef lead = context().actorOf(DecreePresident.props(cluster, txid));
                    lead.tell(new PaxosAPI.Propose(OLIVE_DAY, txid), self());
                  }
                }

                lastProposedTxid = learnedDecrees.lastKey();

                if (lastDeliveredTxid == lastProposedTxid) {
                  lead();
                }
              }
            })
            .matchAny(m -> stash())
            .build();
  }

  private long receivedDecrees = 0;

  private void sync() {
    LOG.info("Becoming sync");
    receivedDecrees = 0;
    getContext().become(syncing());

    broadcasts.forEachValue(b -> {
      b.tell(new TellMeYourLedger(lastDeliveredTxid), self());
      return true;
    });

    context().system().scheduler().scheduleOnce(
            Duration.create(1, SECONDS),
            self(),
            "Resync",
            context().dispatcher(),
            self()
    );
  }

  /**
   * Local leader has decide that this actor should lead
   * <p>
   * Possible transitions:
   * <ol>
   * <li>Lead -> priest, on NewLeader</li>
   * <li>Lead -> sync, on "You are not my leader" messages</li>
   */
  private Receive leader() {
    return ReceiveBuilder.create()
            //common
            .match(TellMeYourLedger.class, this::tellMyLedger)
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            //leader specific
            .match(Broadcast.class, this::onLeaderBroadcast)
            .build();
  }

  private void lead() {
    LOG.info("Becoming lead");
    unstashAll();
    getContext().become(leader());
  }

  private void onLeaderBroadcast(Broadcast<?> broadcast) {
    lastProposedTxid++;
    final ActorRef lead = context().actorOf(DecreePresident.props(cluster, lastProposedTxid));
    lead.tell(new PaxosAPI.Propose(broadcast.value, lastProposedTxid), self());
  }

  /**
   * Redirecting broadcast messages to the current leader, learning, delivering messages
   */
  private Receive priest() {
    return ReceiveBuilder.create()
            //common
            .match(TellMeYourLedger.class, this::tellMyLedger)
            .match(ElectorAPI.NewLeader.class, this::onNewLeader)
            .match(PaxosMessage.class, this::onPaxosMessage)
            .match(PaxosAPI.Decide.class, this::onDecide)
            .build();
  }

  private void pray() {
    LOG.info("Becoming priest");
    unstashAll();
    getContext().become(priest());
  }

  //Common methods:

  private void tellMyLedger(TellMeYourLedger tellMeYourLedger) {
    final SortedMap<Long, Object> myDecrees = learnedDecrees.tailMap(tellMeYourLedger.fromTxid);
    sender().tell(new LearnedDecrees(tellMeYourLedger.fromTxid, myDecrees), self());
  }

  private void onNewLeader(ElectorAPI.NewLeader newLeader) {
    if (newLeader.leader == this.id) {
      sync();
    } else {
      pray();
    }
  }

  private void onDecide(PaxosAPI.Decide decide) {
    if (decide.txid <= lastDeliveredTxid) {
      LOG.warning("fromTxid={} is already delivered", decide.txid);
    } else {
      LOG.info("Enqueued fromTxid={}", decide.txid);
      learnedDecrees.put(decide.txid, decide.value);

      // TODO: 9/18/17 clear priests & create them on demand
      //priests.get(decide.txid).tell(PoisonPill.getInstance(), self());
      //priests.remove(decide.txid);
    }

    tryDeliver();
  }

  private void tryDeliver() {
    while (learnedDecrees.containsKey(lastDeliveredTxid + 1)) {
      lastDeliveredTxid++;
      LOG.info("Delivering fromTxid={}", lastDeliveredTxid);
      subscriber.tell(new AtomicBroadcastAPI.Deliver<>(learnedDecrees.get(lastDeliveredTxid)), self());
    }
  }

  //Route paxos messages to the appropriate instance

  private final TLongObjectMap<ActorRef> priests = new TLongObjectHashMap<>();

  private void onPaxosMessage(PaxosMessage paxosMessage) {
    // TODO: 9/18/17 Create priests on demand
    if (priests.containsKey(paxosMessage.txid())) {
      priests.get(paxosMessage.txid()).tell(paxosMessage, sender());
    } else {
      LOG.info("Creating priest for fromTxid={}", paxosMessage.txid());
      final ActorRef priest = context().actorOf(DecreePriest.props(paxosMessage.txid(), self()), String.valueOf(paxosMessage.txid()));
      priests.put(paxosMessage.txid(), priest);
      priest.tell(paxosMessage, sender());
    }
  }
}

enum AtomicBroadcastMessages {
  OLIVE_DAY;

  public static class TellMeYourLedger {
    public final long fromTxid;

    public TellMeYourLedger(long fromTxid) {
      this.fromTxid = fromTxid;
    }

    @Override
    public String toString() {
      return "TellMeYourLedger{" +
              "fromTxid=" + fromTxid +
              '}';
    }
  }

  public static class LearnedDecrees {
    public final long fromTxid;
    public final SortedMap<Long, Object> ledger;

    public LearnedDecrees(long fromTxid, SortedMap<Long, Object> ledger) {
      this.fromTxid = fromTxid;
      this.ledger = ledger;
    }

    @Override
    public String toString() {
      return "LearnedDecrees{" +
              "fromTxid=" + fromTxid +
              ", ledger=" + ledger +
              '}';
    }
  }
}
