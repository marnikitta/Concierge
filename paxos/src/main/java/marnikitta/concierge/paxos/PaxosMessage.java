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

  class LastVote implements PaxosMessage, Comparable<LastVote> {
    public final long txid;
    public final int ballotNumber;
    public final Object vote;

    public LastVote(long txid, int ballotNumber, Object vote) {
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
    public int compareTo(@NotNull LastVote o) {
      return Integer.compare(this.ballotNumber, o.ballotNumber);
    }

    @Override
    public long txid() {
      return txid;
    }
  }

  class BeginBallot implements PaxosMessage {
    public final long txid;
    public final int ballotNumber;
    public final Object decree;

    public BeginBallot(long txid, int ballotNumber, Object decree) {
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

  class Voted implements PaxosMessage {
    public final long txid;
    public final long ballotNumber;
    public final Object vote;

    public Voted(long txid, long ballotNumber, Object vote) {
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

  class AlreadySucceed {
    public final long txid;
    public final Object decree;

    public AlreadySucceed(long txid, Object decree) {
      this.txid = txid;
      this.decree = decree;
    }

    @Override
    public String toString() {
      return "AlreadySucceed{" +
              "txid=" + txid +
              ", decree=" + decree +
              '}';
    }
  }

  enum SpecialValues {
    BLANK,
    OUTDATED_BALLOT_NUMBER
  }
}
