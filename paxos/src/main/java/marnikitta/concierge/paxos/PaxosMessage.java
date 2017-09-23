package marnikitta.concierge.paxos;

import org.jetbrains.annotations.NotNull;

public interface PaxosMessage {
  long txid();

  class NextBallot implements PaxosMessage {
    private final long txid;
    private final int ballotNumber;

    public NextBallot(long txid, int ballotNumber) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
    }

    @Override
    public String toString() {
      return "NextBallot{" +
              "txid=" + txid() +
              ", ballotNumber=" + ballotNumber() +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }

    public int ballotNumber() {
      return ballotNumber;
    }
  }

  class LastVote implements PaxosMessage, Comparable<LastVote> {
    private final long txid;
    private final int ballotNumber;
    private final Object vote;

    public LastVote(long txid, int ballotNumber, Object vote) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "LastVote{" +
              "txid=" + txid() +
              ", ballotNumber=" + ballotNumber() +
              ", vote=" + vote() +
              '}';
    }

    @Override
    public int compareTo(@NotNull LastVote o) {
      return Integer.compare(ballotNumber(), o.ballotNumber());
    }

    @Override
    public long txid() {
      return txid;
    }

    public int ballotNumber() {
      return ballotNumber;
    }

    public Object vote() {
      return vote;
    }
  }

  class BeginBallot implements PaxosMessage {
    private final long txid;
    private final int ballotNumber;
    private final Object decree;

    public BeginBallot(long txid, int ballotNumber, Object decree) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.decree = decree;
    }

    @Override
    public String toString() {
      return "BeginBallot{" +
              "txid=" + txid() +
              ", ballotNumber=" + ballotNumber() +
              ", vote=" + decree() +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }

    public int ballotNumber() {
      return ballotNumber;
    }

    public Object decree() {
      return decree;
    }
  }

  class Voted implements PaxosMessage {
    private final long txid;
    private final long ballotNumber;
    private final Object vote;

    public Voted(long txid, long ballotNumber, Object vote) {
      this.txid = txid;
      this.ballotNumber = ballotNumber;
      this.vote = vote;
    }

    @Override
    public String toString() {
      return "Voted{" +
              "txid=" + txid() +
              ", ballotNumber=" + ballotNumber() +
              ", vote=" + vote() +
              '}';
    }

    @Override
    public long txid() {
      return txid;
    }

    public long ballotNumber() {
      return ballotNumber;
    }

    public Object vote() {
      return vote;
    }
  }

  class Success implements PaxosMessage {
    private final long txid;
    private final Object decree;

    public Success(long txid, Object decree) {
      this.txid = txid;
      this.decree = decree;
    }

    @Override
    public long txid() {
      return txid;
    }

    public Object decree() {
      return decree;
    }
  }

  class AlreadySucceed {
    private final long txid;
    private final Object decree;

    public AlreadySucceed(long txid, Object decree) {
      this.txid = txid;
      this.decree = decree;
    }

    @Override
    public String toString() {
      return "AlreadySucceed{" +
              "txid=" + txid() +
              ", decree=" + decree() +
              '}';
    }

    public long txid() {
      return txid;
    }

    public Object decree() {
      return decree;
    }
  }

  enum SpecialValues {
    BLANK,
    OUTDATED_BALLOT_NUMBER
  }
}
