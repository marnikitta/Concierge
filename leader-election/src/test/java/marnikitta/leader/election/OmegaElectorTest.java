package marnikitta.leader.election;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class OmegaElectorTest {
  private ActorSystem system;

  @BeforeSuite
  public void initSystem() {
    system = ActorSystem.create();
  }

  @AfterSuite
  public void deinitSystem() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void singleLeader() {
    final Map<TestKit, ActorRef> electors = Stream.generate(() -> new TestKit(system)).limit(100)
            .collect(toMap(Function.identity(), kit -> system.actorOf(OmegaElector.props(kit.getRef()))));

    electors.values().forEach(e -> e.tell(new ElectorAPI.RegisterElectors(electors.values()), null));

    final ActorRef min = Collections.min(electors.values());
    electors.keySet().forEach(kit -> kit.expectMsg(new ElectorAPI.NewLeader(min)));
  }

  @Test
  public void fallingOneByOne() {
    final SortedMap<ActorRef, TestKit> electors = Stream.generate(() -> new TestKit(system)).limit(100)
            .collect(toMap(
                    kit -> system.actorOf(OmegaElector.props(kit.getRef())),
                    Function.identity(),
                    (u, v) -> v,
                    TreeMap::new
                    )
            );

    electors.keySet().forEach(e -> e.tell(new ElectorAPI.RegisterElectors(electors.keySet()), null));

    while (!electors.isEmpty()) {
      final ActorRef currentMin = electors.firstKey();
      electors.values().forEach(kit -> kit.expectMsg(new ElectorAPI.NewLeader(currentMin)));
      currentMin.tell(PoisonPill.getInstance(), ActorRef.noSender());
      electors.remove(electors.firstKey());
    }
  }
}