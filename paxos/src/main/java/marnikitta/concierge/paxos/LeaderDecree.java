package marnikitta.concierge.paxos;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static marnikitta.concierge.paxos.PaxosMessages.*;

public final class LeaderDecree extends AbstractActor {
  private final long txid;
  private final int ballot = 1;
  private final List<ActorRef> priests;

  private LeaderDecree(Collection<ActorRef> priests, long txid) {
    this.txid = txid;
    this.priests = new ArrayList<>(priests);
  }

  @Override
  public void preStart() throws Exception {
    priests.forEach(p -> p.tell(new NextBallot(txid, ballot), self()));
    getContext().become(nextBallotSent());
    super.preStart();
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create().build();
  }

  private final Map<ActorRef, LastVote<?>> lastVotes = new HashMap<>();

  private Object lockedValue;

  private Receive nextBallotSent() {
    return ReceiveBuilder.create().match(LastVote.class, lastVote -> {
      assert lastVote.ballot == ballot;

      lastVotes.put(sender(), lastVote);

      if (lastVotes.size() > priests.size() / 2) {
        final LastVote<?> winner = Collections.max(lastVotes.values());
        this.lockedValue = winner.vote;
        priests.forEach(p -> p.tell(new BeginBallot<>(txid, ballot, winner.vote), self()));
        getContext().become(beginBallotSent());
      }
    }).build();
  }

  private final Set<ActorRef> votedRefs = new HashSet<>();

  private Receive beginBallotSent() {
    return ReceiveBuilder.create().match(Voted.class, voted -> {
      assert voted.ballot == ballot;

      votedRefs.add(sender());

      if (votedRefs.size() > priests.size() / 2) {
        priests.forEach(p -> p.tell(new Success(txid, ballot), self()));
      }
    }).build();
  }
}
