package marnikitta.leader.election;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class OmegaElectorTest {

  @Test
  public void testElection() throws Exception {
    final ActorSystem system = ActorSystem.create();

    final List<ActorRef> actors = IntStream.range(0, 10)
            .mapToObj(i -> system.actorOf(OmegaElector.props(), "elector" + i))
            .collect(Collectors.toList());

    for (ActorRef a : actors) {
      actors.forEach(part -> a.tell(new ElectorAPI.AddParticipant(part), null));
    }

    for (ActorRef actor : actors) {
      TimeUnit.SECONDS.sleep(10);
      actor.tell(PoisonPill.getInstance(), null);
    }

    TimeUnit.SECONDS.sleep(10000);
  }
}