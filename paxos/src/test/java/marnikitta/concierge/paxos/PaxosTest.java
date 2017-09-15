package marnikitta.concierge.paxos;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.common.Cluster;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PaxosTest {
  public static final int PRIESTS_COUNT = 17;
  private ActorSystem system;
  public static final int MINORITY = PRIESTS_COUNT / 2 - 1;

  @BeforeSuite
  public void initSystem() {
    system = ActorSystem.create();
  }

  @AfterSuite
  public void deinitSystem() {
    TestKit.shutdownActorSystem(system);
  }

  @Test
  public void testSimplePropose() throws Exception {
    final String prefix = "simplePaxos";
    final List<TestPriest> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final Map<Long, ActorPath> priestsPaths = testPriests.stream().collect(toMap(p -> p.id, p -> p.path));

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());

    final List<TestKit> kits = testPriests.stream().map(p -> p.kit).collect(toList());

    kits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testDoublePropose() throws Exception {
    final String prefix = "doublePropose";
    final List<TestPriest> testPriests = LongStream.range(0, PRIESTS_COUNT)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final Map<Long, ActorPath> priestsPaths = testPriests.stream().collect(toMap(p -> p.id, p -> p.path));
    final List<TestKit> kits = testPriests.stream().map(p -> p.kit).collect(toList());

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    kits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));

    final ActorRef anotherLeader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE1", 1), ActorRef.noSender());
    kits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMajorityPropose() throws Exception {
    final String prefix = "majorityPropose";
    final List<TestPriest> majorityTestPriests = LongStream.range(0, PRIESTS_COUNT - MINORITY)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final List<TestPriest> minorityTestPriests = LongStream.range(PRIESTS_COUNT - MINORITY, PRIESTS_COUNT)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final Map<Long, ActorPath> priestsPaths = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .collect(toMap(p -> p.id, p -> p.path));

    final List<TestKit> majorityKits = majorityTestPriests.stream().map(p -> p.kit).collect(toList());

    minorityTestPriests.forEach(p -> p.priest.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    majorityKits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMajorityDoublePropose() throws Exception {
    final String prefix = "majorityDoublePropose";
    final List<TestPriest> majorityTestPriests = LongStream.range(0, PRIESTS_COUNT - MINORITY)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final List<TestPriest> minorityTestPriests = LongStream.range(PRIESTS_COUNT - MINORITY, PRIESTS_COUNT)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final Map<Long, ActorPath> priestsPaths = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .collect(toMap(p -> p.id, p -> p.path));

    final List<TestKit> majorityKits = majorityTestPriests.stream().map(p -> p.kit).collect(toList());
    final List<TestKit> allKits = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .map(p -> p.kit).collect(toList());

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    allKits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));

    minorityTestPriests.forEach(p -> p.priest.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef anotherLeader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE1", 1), ActorRef.noSender());
    majorityKits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMinorityPropose() throws Exception {
    final String prefix = "minorityPropose";
    final List<TestPriest> majorityTestPriests = LongStream.range(0, PRIESTS_COUNT - MINORITY)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final List<TestPriest> minorityTestPriests = LongStream.range(PRIESTS_COUNT - MINORITY, PRIESTS_COUNT)
            .boxed()
            .map(l -> testPriest(prefix, l))
            .collect(toList());

    final Map<Long, ActorPath> priestsPaths = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .collect(toMap(p -> p.id, p -> p.path));

    final List<TestKit> majorityKits = majorityTestPriests.stream().map(p -> p.kit).collect(toList());

    majorityTestPriests.forEach(p -> p.priest.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    majorityKits.forEach(kit -> kit.expectNoMsg(Duration.create(1, SECONDS)));
  }

  private TestPriest testPriest(String prefix, long id) {
    final TestKit kit = new TestKit(system);
    return new TestPriest(
            id,
            system.actorOf(DecreePriest.props(1, kit.getRef()), prefix + id),
            system.child(prefix + id),
            kit
    );
  }

  private static class TestPriest {
    public final long id;
    public final ActorRef priest;
    public final ActorPath path;
    public final TestKit kit;

    public TestPriest(long id, ActorRef priest, ActorPath path, TestKit kit) {
      this.id = id;
      this.priest = priest;
      this.path = path;
      this.kit = kit;
    }
  }
}