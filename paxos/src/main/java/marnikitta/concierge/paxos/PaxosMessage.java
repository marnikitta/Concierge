package marnikitta.concierge.paxos;

import org.jetbrains.annotations.NotNull;

public interface PaxosMessage {
  long txid();

  class NextBallot implements PaxosMessage {
    public final long txid;
    public final int ballotNumber;

    public NextBallot(long txid, int ballotNumber) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
    }

    @Override
    public String toString() {
      return "NextBallot{" +
              "txid=" + txid +
              ", ballotNumber=" + ballotNumber +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class LastVote<T> implements PaxosMessage, Comparable<LastVote<?>> {
    public final long txid;
    public final int ballotNumber;
    public final T vote;

    public LastVote(long txid, int ballotNumber, T vote) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "LastVote{" +
              "txid=" + txid +
              ", ballotNumber=" + ballotNumber +
              ", vote=" + vote +
              '}';
    }

    @Override
    public int compareTo(@NotNull LastVote<?> o) {
      return Integer.compare(this.ballotNumber, o.ballotNumber);
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class BeginBallot<T> implements PaxosMessage {
    public final long txid;
    public final int ballotNumber;
    public final T decree;

    public BeginBallot(long txid, int ballotNumber, T decree) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.decree = decree;
    }

    @Override
    public String toString() {
      return "BeginBallot{" +
              "txid=" + txid +
              ", ballotNumber=" + ballotNumber +
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
    public final long ballotNumber;
    public final T vote;

    public Voted(long txid, long ballotNumber, T vote) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "Voted{" +
              "txid=" + txid +
              ", ballotNumber=" + ballotNumber +
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
    public final long ballotNumber;

    public Success(long txid, long ballotNumber) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
    }

    @Override
    public String toString() {
      return "Success{" +
              "txid=" + txid +
              ", ballotNumber=" + ballotNumber +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  enum SpecialValues {
    BLANK,
    OUTDATED_BALLOT_NUMBER
  }
}
