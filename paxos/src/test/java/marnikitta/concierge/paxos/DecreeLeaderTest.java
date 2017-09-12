package marnikitta.concierge.paxos;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.*;
import static java.util.stream.Collectors.toList;

public class DecreeLeaderTest {
  @Test
  public void testSimplePropose() throws Exception {
    final ActorSystem system = ActorSystem.create();
    final List<ActorRef> priests = IntStream.range(0, 17).mapToObj(i -> system.actorOf(DecreePriest.props(1), "priest" + i))
            .collect(toList());

    final ActorRef leader = system.actorOf(DecreeLeader.props(priests, 1, "VALUE"));

    SECONDS.sleep(100);
  }
}