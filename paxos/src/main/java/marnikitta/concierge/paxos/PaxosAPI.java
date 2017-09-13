package marnikitta.concierge.paxos;

import java.util.Objects;

public interface PaxosAPI {
  class Propose<T> {
    public final T value;
    public final long txid;

    public Propose(T value, long txid) {
      this.value = value;
      this.txid = txid;
    }

    @Override
    public String toString() {
      return "Propose{" +
              "value=" + value +
              ", txid=" + txid +
              '}';
    }
  }

  class Decide<T> {
    public final T value;
    public final long txid;

    public Decide(T value, long txid) {
      this.value = value;
      this.txid = txid;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Decide<?> decide = (Decide<?>) o;
      return txid == decide.txid &&
              Objects.equals(value, decide.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, txid);
    }

    @Override
    public String toString() {
      return "Decide{" +
              "value=" + value +
              ", txid=" + txid +
              '}';
    }
  }
}
