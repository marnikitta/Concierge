package marnikitta.leader.election;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.common.ConciergeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.LongStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

public class OmegaElectorTest extends ConciergeTest {
  @Test
  public void defaultLeader() {
    final int electorsCount = 20;
    final Map<Long, ActorPath> paths = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(Function.identity(), i -> system.child("defaultelector" + i)));

    final Map<Long, TestKit> kits = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(Function.identity(), i -> new TestKit(system)));

    final Map<Long, ActorRef> electors = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(
                    Function.identity(),
                    i -> system.actorOf(OmegaElector.props(i, kits.get(i).getRef(), new Cluster(paths)), "defaultelector" + i)
            ));

    final long min = Collections.min(electors.keySet());
    kits.values().forEach(kit -> kit.expectMsg(new ElectorAPI.NewLeader(min)));
  }


  @Test
  public void failingOneByOne() throws InterruptedException {
    final int electorsCount = 20;
    final Map<Long, ActorPath> paths = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(Function.identity(), i -> system.child("failingelector" + i)));

    final Map<Long, TestKit> kits = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(Function.identity(), i -> new TestKit(system)));

    final Map<Long, ActorRef> electors = LongStream.range(0, electorsCount)
            .boxed()
            .collect(toMap(
                    Function.identity(),
                    i -> system.actorOf(OmegaElector.props(i, kits.get(i).getRef(), new Cluster(paths)), "failingelector" + i)
            ));

    final SortedSet<Long> alive = new TreeSet<>(paths.keySet());

    SECONDS.sleep(5);

    while (!alive.isEmpty()) {
      final long min = alive.first();
      alive.stream().map(kits::get).forEach(kit -> kit.expectMsg(new ElectorAPI.NewLeader(min)));

      electors.get(min).tell(PoisonPill.getInstance(), ActorRef.noSender());
      alive.remove(min);
    }
  }
}
