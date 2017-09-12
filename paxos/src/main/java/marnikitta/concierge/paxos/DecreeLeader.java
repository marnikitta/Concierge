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

  private final Object proposal;

  private DecreeLeader(Collection<ActorRef> priests, long txid) {
    this(priests, txid, SpecialValues.NO_OP);
  }

  private DecreeLeader(Collection<ActorRef> priests, long txid, Object proposal) {
    this.txid = txid;
    this.priests = new ArrayList<>(priests);
    this.proposal = proposal;
  }

  public static Props props(Collection<ActorRef> priests, long txid) {
    return Props.create(DecreeLeader.class, priests, txid);
  }

  public static Props props(Collection<ActorRef> priests, long txid, Object proposal) {
    return Props.create(DecreeLeader.class, priests, txid, proposal);
  }

  @Override
  public void preStart() throws Exception {
    broadcastNextBallot(1);
    super.preStart();
  }

  private int ballot = -1;

  private void broadcastNextBallot(int ballot) {
    this.ballot = ballot;
    priests.forEach(p -> p.tell(new NextBallot(txid, ballot), self()));
    getContext().become(nextBallotSent());
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create().build();
  }

  private final Map<ActorRef, LastVote<?>> lastVotes = new HashMap<>();

  private Object lockedValue;

  private Receive nextBallotSent() {
    return ReceiveBuilder.create().match(
            LastVote.class,
            lastVote -> lastVote.ballot == ballot,
            lastVote -> {
              lastVotes.put(sender(), lastVote);

              if (lastVotes.size() > priests.size() / 2) {
                final LastVote<?> winner = Collections.max(lastVotes.values());

                if (winner.vote == SpecialValues.NO_OP) {
                  lockedValue = proposal;
                } else if (winner.vote == SpecialValues.OUTDATED_BALLOT) {
                  broadcastNextBallot(1);
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
            voted -> ballot == voted.ballot,
            voted -> {
              votedRefs.add(sender());

              if (votedRefs.size() > priests.size() / 2) {
                priests.forEach(p -> p.tell(new Success(txid, ballot), self()));
              }
            }
    ).build();
  }
}
