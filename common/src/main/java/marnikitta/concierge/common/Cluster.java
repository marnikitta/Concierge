package marnikitta.concierge.common;

import akka.actor.ActorPath;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public final class Cluster {
  private final Map<Long, ActorPath> paths;

  public Cluster(Map<Long, ActorPath> paths, String suffix) {
    this.paths = paths.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().child(suffix)));
  }

  public Cluster(Map<Long, ActorPath> paths) {
    this.paths = new HashMap<>(paths);
  }

  @Override
  public String toString() {
    return "Cluster{" +
            "paths=" + paths() +
            '}';
  }

  public Map<Long, ActorPath> paths() {
    return paths;
  }
}
