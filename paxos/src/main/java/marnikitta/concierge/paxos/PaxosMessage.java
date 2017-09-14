package marnikitta.concierge.paxos;

import org.jetbrains.annotations.NotNull;

public interface PaxosMessage {
  long txid();

  class NextBallot implements PaxosMessage {
    public final long txid;
    public final int ballot;

    public NextBallot(long txid, int ballot) {
      this.txid = txid;
      this.ballot = ballot;
    }

    @Override
    public String toString() {
      return "NextBallot{" +
              "txid=" + txid +
              ", ballot=" + ballot +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class LastVote<T> implements PaxosMessage, Comparable<LastVote<?>> {
    public final long txid;
    public final int ballot;
    public final T vote;

    public LastVote(long txid, int ballot, T vote) {
      this.txid = txid;
      this.ballot = ballot;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "LastVote{" +
              "txid=" + txid +
              ", ballot=" + ballot +
              ", vote=" + vote +
              '}';
    }

    @Override
    public int compareTo(@NotNull LastVote<?> o) {
      return Integer.compare(this.ballot, o.ballot);
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class BeginBallot<T> implements PaxosMessage {
    public final long txid;
    public final int ballot;
    public final T decree;

    public BeginBallot(long txid, int ballot, T decree) {
      this.txid = txid;
      this.ballot = ballot;
      this.decree = decree;
    }

    @Override
    public String toString() {
      return "BeginBallot{" +
              "txid=" + txid +
              ", ballot=" + ballot +
              ", vote=" + decree +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class Voted<T> implements PaxosMessage {
    public final long txid;
    public final long ballot;
    public final T vote;

    public Voted(long txid, long ballot, T vote) {
      this.txid = txid;
      this.ballot = ballot;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "Voted{" +
              "txid=" + txid +
              ", ballot=" + ballot +
              ", vote=" + vote +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class Success implements PaxosMessage {
    public final long txid;
    public final long ballot;

    public Success(long txid, long ballot) {
      this.txid = txid;
      this.ballot = ballot;
    }

    @Override
    public String toString() {
      return "Success{" +
              "txid=" + txid +
              ", ballot=" + ballot +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  enum SpecialValues {
    NO_VALUE,
    OUTDATED_BALLOT
  }
}