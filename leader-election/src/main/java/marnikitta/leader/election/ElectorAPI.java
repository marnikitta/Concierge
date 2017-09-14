package marnikitta.leader.election;

import akka.actor.ActorRef;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public interface ElectorAPI {
  class NewLeader {
    public final long leader;

    public NewLeader(long leader) {
      this.leader = leader;
    }

    @Override
    public String toString() {
      return "NewLeader{" + "leader=" + leader + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final NewLeader newLeader = (NewLeader) o;
      return leader == newLeader.leader;
    }

    @Override
    public int hashCode() {
      return Objects.hash(leader);
    }
  }
}
