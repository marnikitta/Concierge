package marnikitta.concierge.paxos;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import org.jetbrains.annotations.Nullable;

import static marnikitta.concierge.paxos.PaxosMessage.AlreadySucceed;
import static marnikitta.concierge.paxos.PaxosMessage.BeginBallot;
import static marnikitta.concierge.paxos.PaxosMessage.LastVote;
import static marnikitta.concierge.paxos.PaxosMessage.NextBallot;
import static marnikitta.concierge.paxos.PaxosMessage.SpecialValues;
import static marnikitta.concierge.paxos.PaxosMessage.Success;
import static marnikitta.concierge.paxos.PaxosMessage.Voted;

public final class DecreePriest extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);
  private final ActorRef subscriber;
  private final long txid;

  @Nullable
  private Object decision = null;

  private Object lastVote = SpecialValues.BLANK;
  private int lastBallot = 0;

  private DecreePriest(long txid, ActorRef subscriber) {
    this.txid = txid;
    this.subscriber = subscriber;
  }

  public static Props props(long txid, ActorRef subscriber) {
    return Props.create(DecreePriest.class, txid, subscriber);
  }

  @Override
  public Receive createReceive() {
    if (decision == null) {
      return ReceiveBuilder.create()
              .match(NextBallot.class, nextBallot -> {
                if (nextBallot.ballotNumber() > lastBallot) {
                  lastBallot = nextBallot.ballotNumber();
                  sender().tell(new LastVote(txid, lastBallot, lastVote), self());
                } else {
                  sender().tell(new LastVote(txid, lastBallot, SpecialValues.OUTDATED_BALLOT_NUMBER), self());
                  LOG.warning(
                          "NextBallot: Proposer has outdated ballotNumber number proposer={}, ballotNumber={}, currentBallot={}",
                          sender(), nextBallot.ballotNumber(), lastBallot
                  );
                }
              })
              .match(BeginBallot.class, beginBallot -> {
                //FIXME: equal ballotNumber numbers may cause collisions, but they shouldn't pass NextBallot
                if (beginBallot.ballotNumber() == lastBallot) {
                  lastVote = beginBallot.decree();
                  sender().tell(new Voted(txid, beginBallot.ballotNumber(), beginBallot.decree()), self());
                } else {
                  sender().tell(new Voted(txid, lastBallot, SpecialValues.OUTDATED_BALLOT_NUMBER), self());
                  LOG.warning(
                          "BeginBallot: Proposer has outdated ballotNumber number proposer={}, ballotNumber={}, currentBallot={}",
                          sender(), beginBallot.ballotNumber(), lastBallot
                  );
                }
              })
              .match(Success.class, success -> {
                LOG.info("Learned {} for txid={}", success.decree(), txid);
                decision = success.decree();
                subscriber.tell(new PaxosAPI.Decide(decision, txid), self());
                getContext().become(success());
              })
              .build();
    } else {
      return success();
    }
  }

  private Receive success() {
    return ReceiveBuilder.create()
            .match(Success.class, m -> {})
            .matchAny(m -> sender().tell(new AlreadySucceed(txid, decision), self()))
            .build();
  }
}
