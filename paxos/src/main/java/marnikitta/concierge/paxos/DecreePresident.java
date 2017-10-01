package marnikitta.concierge.paxos;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import marnikitta.concierge.common.Cluster;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static marnikitta.concierge.paxos.PaxosMessage.AlreadySucceed;
import static marnikitta.concierge.paxos.PaxosMessage.BeginBallot;
import static marnikitta.concierge.paxos.PaxosMessage.LastVote;
import static marnikitta.concierge.paxos.PaxosMessage.NextBallot;
import static marnikitta.concierge.paxos.PaxosMessage.SpecialValues;
import static marnikitta.concierge.paxos.PaxosMessage.Success;
import static marnikitta.concierge.paxos.PaxosMessage.Voted;

public final class DecreePresident extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final long txid;
  private final Set<ActorSelection> priests;

  private DecreePresident(Cluster priests, long txid) {
    this.txid = txid;
    this.priests = priests.paths().stream()
            .map(path -> context().actorSelection(path))
            .collect(toSet());
  }

  public static Props props(Cluster priests, long txid) {
    return Props.create(DecreePresident.class, priests, txid);
  }

  @Nullable
  private Object proposal = null;

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(
                    PaxosAPI.Propose.class,
                    propose -> propose.txid() == txid,
                    propose -> {
                      LOG.info("Received proposal={}", propose);
                      this.proposal = propose.value();
                      broadcastNextBallot(1);
                    }
            )
            .build();
  }

  private int lastTried = -1;

  private void broadcastNextBallot(int ballotNumber) {
    LOG.info("Broadcasting NextBallot lastTried={}, txid={}", ballotNumber, txid);

    this.lastTried = ballotNumber;
    priests.forEach(p -> p.tell(new NextBallot(txid, ballotNumber), self()));
    getContext().become(nextBallotSent());
  }

  private final Map<ActorRef, LastVote> lastVotes = new HashMap<>();

  private Receive nextBallotSent() {
    return ReceiveBuilder.create()
            .match(AlreadySucceed.class, this::onAlreadySucceed)
            .match(LastVote.class,
                    lastVote -> lastVote.ballotNumber() == lastTried && lastVote.txid() == txid,
                    lastVote -> {
                      LOG.info("Received last vote from {}", lastVote);
                      lastVotes.put(sender(), lastVote);

                      if (lastVotes.size() > priests.size() / 2) {
                        LOG.info("There is a quorum");
                        final LastVote winner = Collections.max(lastVotes.values());

                        final Object lockedValue;

                        if (winner.vote() == SpecialValues.BLANK) {
                          lockedValue = proposal;
                        } else if (winner.vote() == SpecialValues.OUTDATED_BALLOT_NUMBER) {
                          LOG.info("Seems that I have outdated ballot number");
                          broadcastNextBallot(winner.ballotNumber() + 1);
                          return;
                        } else {
                          lockedValue = winner.vote();
                        }

                        priests.forEach(p -> p.tell(new BeginBallot(txid, lastTried, lockedValue), self()));
                        getContext().become(beginBallotSent());
                      }
                    })
            .build();
  }

  private void onAlreadySucceed(AlreadySucceed alreadySucceed) {
    LOG.warning("Txid={} already succeed", alreadySucceed.txid());
    priests.forEach(p -> p.tell(new Success(txid, alreadySucceed.decree()), self()));
    context().stop(self());
  }

  private final Set<Voted> votes = new HashSet<>();

  private Receive beginBallotSent() {
    return ReceiveBuilder.create()
            .match(AlreadySucceed.class, this::onAlreadySucceed)
            .match(Voted.class,
                    voted -> voted.ballotNumber() == lastTried && voted.txid() == txid,
                    voted -> {
                      votes.add(voted);

                      if (votes.size() > priests.size() / 2) {
                        priests.forEach(p -> p.tell(new Success(txid, voted.vote()), self()));
                        context().stop(self());
                      }
                    })
            .build();
  }
}
