package marnikitta.concierge.paxos;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import static marnikitta.concierge.paxos.PaxosMessages.BeginBallot;
import static marnikitta.concierge.paxos.PaxosMessages.LastVote;
import static marnikitta.concierge.paxos.PaxosMessages.NextBallot;
import static marnikitta.concierge.paxos.PaxosMessages.SpecialValues;
import static marnikitta.concierge.paxos.PaxosMessages.Success;
import static marnikitta.concierge.paxos.PaxosMessages.Voted;

public final class DecreePriest extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);
  private final long txid;

  private Object vote = SpecialValues.NO_OP;
  private int maxBallot = 0;

  private DecreePriest(long txid) {
    this.txid = txid;
  }

  public static Props props(long txid) {
    return Props.create(DecreePriest.class, txid);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(NextBallot.class, nextBallot -> {
              if (nextBallot.ballot > maxBallot) {
                maxBallot = nextBallot.ballot;
                sender().tell(new LastVote<>(txid, maxBallot, vote), self());
              } else {
                sender().tell(new LastVote<>(txid, maxBallot, SpecialValues.OUTDATED_BALLOT), self());
                LOG.warning(
                        "Proposer has outdated ballot number proposer={}, ballot={}, currentBallot={}",
                        sender(), nextBallot.ballot, maxBallot
                );
              }
            })
            .match(BeginBallot.class, beginBallot -> {
              //FIXME: equal ballot numbers may cause collisions, but they shouldn't pass NextBallot
              if (beginBallot.ballot == maxBallot) {
                vote = beginBallot.decree;
                sender().tell(new Voted<>(txid, beginBallot.ballot, beginBallot.decree), self());
              } else {
                sender().tell(new Voted<>(txid, maxBallot, SpecialValues.OUTDATED_BALLOT), self());
                LOG.warning(
                        "Proposer has outdated ballot number proposer={}, ballot={}, currentBallot={}",
                        sender(), beginBallot.ballot, maxBallot
                );
              }
            })
            .match(Success.class, success -> {
              LOG.info("Learned {} for txid={}", vote, txid);
              context().parent().tell(new PaxosAPI.Decide<>(vote), self());
            })
            .build();

  }
}
