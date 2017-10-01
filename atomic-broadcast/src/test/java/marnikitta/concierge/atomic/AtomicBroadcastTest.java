package marnikitta.concierge.atomic;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.common.ConciergeTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class AtomicBroadcastTest extends ConciergeTest {
  public static final int PRIESTS_COUNT = 17;
  public static final int MINORITY = PRIESTS_COUNT / 2 - 1;

  @Test
  public void singleLeaderBroadcastTest() {
    final String prefix = "simpleLeader";
    final Set<ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed().map(l -> system.child(prefix + l))
            .collect(Collectors.toSet());

    final List<TestBroadcast> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testBroadcast(prefix, l, new Cluster(broadcastPaths)))
            .collect(toList());

    final List<String> decrees = Stream
            .generate(UUID::randomUUID)
            .map(UUID::toString)
            .limit(1000)
            .collect(toList());

    for (String v : decrees) {
      testPriests.get(0).broadcast.tell(new AtomicBroadcastAPI.Broadcast(v), ActorRef.noSender());
      testPriests.forEach(p -> p.kit.expectMsg(new AtomicBroadcastAPI.Deliver(v)));
    }
  }

  @Test
  public void killOneByOneTest() {
    final String prefix = "killOneByOneTest";
    final Set<ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> system.child(prefix + l))
            .collect(Collectors.toSet());

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

      for (String v : decrees) {
        currentLeader.broadcast.tell(new AtomicBroadcastAPI.Broadcast(v), ActorRef.noSender());
        testPriests.forEach(p -> p.kit.expectMsg(new AtomicBroadcastAPI.Deliver(v)));
      }

      currentLeader.broadcast.tell(PoisonPill.getInstance(), ActorRef.noSender());
      testPriests.remove(currentLeader);
    }
  }

  @Test
  public void awakenMinorityTest() {
    final String prefix = "awakenMinorityTest";
    final Set<ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> system.child(prefix + l))
            .collect(Collectors.toSet());

    final List<TestBroadcast> majority = LongStream.range(0, PRIESTS_COUNT - MINORITY - 1)
            .boxed()
            .map(l -> testBroadcast(prefix, l, new Cluster(broadcastPaths)))
            .collect(toList());

    final List<TestBroadcast> alive = new ArrayList<>(majority);
    final List<String> decrees = new ArrayList<>();

    for (long id = PRIESTS_COUNT - MINORITY - 1; id < PRIESTS_COUNT; ++id) {
      final TestBroadcast awakenGuy = testBroadcast(prefix, id, new Cluster(broadcastPaths));
      final List<String> pileForAwaken = Stream
              .generate(UUID::randomUUID)
              .map(UUID::toString)
              .limit(1000)
              .collect(toList());

      pileForAwaken.forEach(d -> awakenGuy.broadcast.tell(new AtomicBroadcastAPI.Broadcast(d), ActorRef.noSender()));

      for (String v : pileForAwaken) {
        alive.forEach(p -> p.kit.expectMsg(new AtomicBroadcastAPI.Deliver(v)));
      }

      decrees.addAll(pileForAwaken);

      for (String d : decrees) {
        awakenGuy.kit.expectMsg(new AtomicBroadcastAPI.Deliver(d));
      }
      alive.add(awakenGuy);
    }
  }

  @Test
  public void integrityAndTotalOrderTest() {
    final String prefix = "integrityAndTotalOrderTest";
    final Set<ActorPath> broadcastPaths = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> system.child(prefix + l))
            .collect(Collectors.toSet());

    final List<TestBroadcast> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testBroadcast(prefix, l, new Cluster(broadcastPaths)))
            .collect(toList());

    final List<String> decrees = Stream
            .generate(UUID::randomUUID)
            .map(UUID::toString)
            .limit(1000)
            .collect(toList());

    final Random rd = new Random();
    for (String v : decrees) {
      testPriests.get(rd.nextInt(testPriests.size())).broadcast
              .tell(new AtomicBroadcastAPI.Broadcast(v), ActorRef.noSender());
    }

    final Set<List<String>> resultSet = new HashSet<>();

    testPriests.stream().map(t -> t.kit).forEach(k -> {
      final List<String> received = new ArrayList<>();
      k.receiveWhile(
              Duration.create(1, MINUTES),
              Duration.create(10, SECONDS),
              1000,
              o -> received.add((String) ((AtomicBroadcastAPI.Deliver) o).value())
      );

      resultSet.add(received);
    });

    Assert.assertEquals(resultSet.size(), 1);

    final List<String> representative = resultSet.stream().findAny().orElseThrow(IllegalStateException::new);
    Assert.assertTrue(decrees.containsAll(representative));
    Assert.assertTrue(representative.containsAll(decrees));
  }


  private TestBroadcast testBroadcast(String prefix, long id, Cluster cluster) {
    final TestKit kit = new TestKit(system);
    return new TestBroadcast(
            system.actorOf(AtomicBroadcast.props(kit.getRef(), cluster), prefix + id),
            kit
    );
  }

  private static class TestBroadcast {
    public final ActorRef broadcast;
    public final TestKit kit;

    public TestBroadcast(ActorRef broadcast, TestKit kit) {
      this.broadcast = broadcast;
      this.kit = kit;
    }
  }
}