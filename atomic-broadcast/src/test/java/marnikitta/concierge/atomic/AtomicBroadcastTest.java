package marnikitta.concierge.atomic;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.common.ConciergeTest;
import marnikitta.leader.election.ElectorAPI;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class AtomicBroadcastTest extends ConciergeTest {
  public static final int PRIESTS_COUNT = 17;
  public static final int MINORITY = PRIESTS_COUNT / 2 - 1;

  @Test
  public void simpleLeaderBroadcast() {
    final String prefix = "simpleLeader";
    final Map<Long, ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed().collect(toMap(Function.identity(), l -> system.child(prefix + l)));

    final List<TestBroadcast> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testBroadcast(prefix, l, new Cluster(broadcastPaths)))
            .collect(toList());

    final List<String> decrees = Stream
            .generate(UUID::randomUUID)
            .map(UUID::toString)
            .limit(1000)
            .collect(toList());
    testPriests.forEach(t -> t.broadcast.tell(new ElectorAPI.NewLeader(0), ActorRef.noSender()));

    for (String v : decrees) {
      testPriests.get(0).broadcast.tell(new AtomicBroadcastAPI.Broadcast<>(v), ActorRef.noSender());
      testPriests.forEach(p -> p.kit.expectMsg(new AtomicBroadcastAPI.Deliver<>(v)));
    }
  }

  @Test
  public void killOneByOne() {
    final String prefix = "killOneByOne";
    final Map<Long, ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed().collect(toMap(Function.identity(), l -> system.child(prefix + l)));

    final List<TestBroadcast> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testBroadcast(prefix, l, new Cluster(broadcastPaths)))
            .collect(toList());

    while (testPriests.size() > PRIESTS_COUNT / 2) {
      final TestBroadcast currentLeader = testPriests.get(0);

      final List<String> decrees = Stream
              .generate(UUID::randomUUID)
              .map(UUID::toString)
              .limit(1000)
              .collect(toList());

      testPriests.forEach(t -> t.broadcast.tell(new ElectorAPI.NewLeader(currentLeader.id), ActorRef.noSender()));

      for (String v : decrees) {
        currentLeader.broadcast.tell(new AtomicBroadcastAPI.Broadcast<>(v), ActorRef.noSender());
        testPriests.forEach(p -> p.kit.expectMsg(new AtomicBroadcastAPI.Deliver<>(v)));
      }

      currentLeader.broadcast.tell(PoisonPill.getInstance(), ActorRef.noSender());
      testPriests.remove(currentLeader);
    }
  }

  private TestBroadcast testBroadcast(String prefix, long id, Cluster cluster) {
    final TestKit kit = new TestKit(system);
    return new TestBroadcast(
            id,
            system.actorOf(AtomicBroadcast.props(id, kit.getRef(), cluster), prefix + id),
            system.child(prefix + id),
            kit
    );
  }

  private static class TestBroadcast {
    public final long id;
    public final ActorRef broadcast;
    public final ActorPath path;
    public final TestKit kit;

    public TestBroadcast(long id, ActorRef broadcast, ActorPath path, TestKit kit) {
      this.id = id;
      this.broadcast = broadcast;
      this.path = path;
      this.kit = kit;
    }
  }
}