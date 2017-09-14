package marnikitta.failure.detector;

public interface DetectorAPI {
  class Suspect {
    public final long theSuspect;

    public Suspect(long theSuspect) {
      this.theSuspect = theSuspect;
    }

    @Override
    public String toString() {
      return "Suspect{" + "theSuspect=" + theSuspect + '}';
    }
  }

  class Restore {
    public final long theSuspect;

    public Restore(long theSuspect) {
      this.theSuspect = theSuspect;
    }

    @Override
    public String toString() {
      return "Restore{" + "theSuspect=" + theSuspect + '}';
    }
  }
}
