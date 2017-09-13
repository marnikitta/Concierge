package marnikitta.concierge.paxos;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static marnikitta.concierge.paxos.PaxosMessages.BeginBallot;
import static marnikitta.concierge.paxos.PaxosMessages.LastVote;
import static marnikitta.concierge.paxos.PaxosMessages.NextBallot;
import static marnikitta.concierge.paxos.PaxosMessages.SpecialValues;
import static marnikitta.concierge.paxos.PaxosMessages.Success;
import static marnikitta.concierge.paxos.PaxosMessages.Voted;

public final class DecreeLeader extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);

  private final long txid;
  private final List<ActorRef> priests;

  private DecreeLeader(Collection<ActorRef> priests, long txid) {
    this.txid = txid;
    this.priests = new ArrayList<>(priests);
  }

  public static Props props(Collection<ActorRef> priests, long txid) {
    return Props.create(DecreeLeader.class, priests, txid);
  }

  private Object proposal;

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(
                    PaxosAPI.Propose.class,
                    propose -> propose.txid == txid,
                    propose -> {
                      LOG.info("Received proposal={}", propose);
                      this.proposal = propose.value;
                      broadcastNextBallot(1);
                    }
            ).build();
  }

  private int ballot = -1;

  private void broadcastNextBallot(int ballot) {
    LOG.info("Broadcasting NextBallot ballot={}, txid={}", ballot, txid);
    this.ballot = ballot;
    priests.forEach(p -> p.tell(new NextBallot(txid, ballot), self()));
    getContext().become(nextBallotSent());
  }

  private final Map<ActorRef, LastVote<?>> lastVotes = new HashMap<>();

  private Object lockedValue;

  private Receive nextBallotSent() {
    return ReceiveBuilder.create().match(
            LastVote.class,
            lastVote -> lastVote.ballot == ballot && lastVote.txid == txid,
            lastVote -> {
              lastVotes.put(sender(), lastVote);

              if (lastVotes.size() > priests.size() / 2) {
                final LastVote<?> winner = Collections.max(lastVotes.values());

                if (winner.vote == SpecialValues.NO_VALUE) {
                  lockedValue = proposal;
                } else if (winner.vote == SpecialValues.OUTDATED_BALLOT) {
                  broadcastNextBallot(winner.ballot + 1);
                  return;
                } else {
                  lockedValue = winner.vote;
                }

                priests.forEach(p -> p.tell(new BeginBallot<>(txid, ballot, lockedValue), self()));
                getContext().become(beginBallotSent());
              }
            }
    ).build();
  }

  private final Set<ActorRef> votedRefs = new HashSet<>();

  private Receive beginBallotSent() {
    return ReceiveBuilder.create().match(
            Voted.class,
            voted -> voted.ballot == ballot && voted.txid == txid,
            voted -> {
              votedRefs.add(sender());

              if (votedRefs.size() > priests.size() / 2) {
                priests.forEach(p -> p.tell(new Success(txid, ballot), self()));
              }
            }
    ).build();
  }
}
