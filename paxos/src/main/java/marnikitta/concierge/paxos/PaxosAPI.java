package marnikitta.concierge.paxos;

import java.util.Objects;

public interface PaxosAPI {
  class Propose {
    private final Object value;
    private final long txid;

    public Propose(Object value, long txid) {
      this.value = value;
      this.txid = txid;
    }

    @Override
    public String toString() {
      return "Propose{" +
              "value=" + value() +
              ", txid=" + txid() +
              '}';
    }

    public Object value() {
      return value;
    }

    public long txid() {
      return txid;
    }
  }

  class Decide {
    private final Object value;
    private final long txid;

    public Decide(Object value, long txid) {
      this.value = value;
      this.txid = txid;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Decide decide = (Decide) o;
      return txid() == decide.txid() &&
              Objects.equals(value(), decide.value());
    }

    @Override
    public int hashCode() {
      return Objects.hash(value(), txid());
    }

    @Override
    public String toString() {
      return "Decide{" +
              "value=" + value() +
              ", txid=" + txid() +
              '}';
    }

    public Object value() {
      return value;
    }

    public long txid() {
      return txid;
    }
  }
}
