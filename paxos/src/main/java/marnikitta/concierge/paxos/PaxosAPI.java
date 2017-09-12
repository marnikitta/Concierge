package marnikitta.concierge.paxos;

public interface PaxosAPI {
  class Propose<T> {
    public final T value;

    public Propose(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Propose{" +
              "value=" + value +
              '}';
    }
  }

  class Decide<T> {
    public final T value;

    public Decide(T value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Decide{" +
              "value=" + value +
              '}';
    }
  }
}
