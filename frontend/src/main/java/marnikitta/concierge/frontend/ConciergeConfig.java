package marnikitta.concierge.frontend;

import akka.actor.ActorPath;
import akka.actor.Address;
import akka.actor.RootActorPath;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import marnikitta.concierge.common.Cluster;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toSet;

public final class ConciergeConfig {
  private final String hostname;
  private final int actorPort;
  private final int clientPort;
  private final long heartbeatMills;
  private final List<String> concierges;

  @JsonCreator
  public ConciergeConfig(@JsonProperty("hostname") String hostname,
                         @JsonProperty("actor_port") int actorPort,
                         @JsonProperty("client_port") int clientPort,
                         @JsonProperty("heartbeat") long heartbeatMills,
                         @JsonProperty("concierges") List<String> concierges) {
    this.hostname = hostname;
    this.actorPort = actorPort;
    this.clientPort = clientPort;
    this.heartbeatMills = heartbeatMills;
    this.concierges = unmodifiableList(concierges);
  }

  public String hostname() {
    return hostname;
  }

  public int actorPort() {
    return actorPort;
  }

  public int clientPort() {
    return clientPort;
  }

  public long heartbeatMills() {
    return heartbeatMills;
  }

  public Cluster cluster() {
    return new Cluster(concierges.stream().map(this::path).collect(toSet()));
  }

  private ActorPath path(String host) {
    final String[] split = host.split(":");
    final String hostname = split[0];
    final int port = Integer.parseInt(split[1]);

    final Address actorSystemAddress = new Address("akka.tcp", "concierge", hostname, port);
    return RootActorPath.apply(actorSystemAddress, "/").child("user");
  }

  @Override
  public String toString() {
    return "ConciergeConfig{" +
            "hostname='" + hostname + '\'' +
            ", actorPort=" + actorPort +
            ", clientPort=" + clientPort +
            ", heartbeatMills=" + heartbeatMills +
            '}';
  }
}
