package marnikitta.concierge.common;

import akka.actor.ActorPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.summarizingDouble;
import static java.util.stream.Collectors.toMap;

public final class Cluster {
  private final Set<ActorPath> paths;

  public Cluster(Set<ActorPath> paths, String suffix) {
    this.paths = paths.stream()
            .map(p -> p.child(suffix))
            .collect(toSet());
  }

  public Cluster(Set<ActorPath> paths) {
    this.paths = new HashSet<>(paths);
  }

  @Override
  public String toString() {
    return "Cluster{" +
            "paths=" + paths() +
            '}';
  }

  public Set<ActorPath> paths() {
    return unmodifiableSet(paths);
  }
}
