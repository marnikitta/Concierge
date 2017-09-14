package marnikitta.concierge.paxos;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
    final Stream<TestKit> priestKits = Stream.generate(() -> new TestKit(system)).limit(PRIESTS_COUNT);

    final Map<TestKit, ActorRef> priests = priestKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final ActorRef leader = system.actorOf(DecreePresident.props(priests.values(), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());

    priests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testDoublePropose() throws Exception {
    final Stream<TestKit> priestKits = Stream.generate(() -> new TestKit(system)).limit(PRIESTS_COUNT);

    final Map<TestKit, ActorRef> priests = priestKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final ActorRef leader = system.actorOf(DecreePresident.props(priests.values(), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());

    priests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));

    final ActorRef anotherLeader = system.actorOf(DecreePresident.props(priests.values(), 1));
    anotherLeader.tell(new PaxosAPI.Propose<>("ANOTHER_VALUE", 1), ActorRef.noSender());

    priests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMajorityPropose() throws Exception {
    final Stream<TestKit> majorityKits = Stream.generate(() -> new TestKit(system)).limit(PRIESTS_COUNT - MINORITY);
    final Stream<TestKit> minorityKits = Stream.generate(() -> new TestKit(system)).limit(MINORITY);

    final Map<TestKit, ActorRef> majorityPriests = majorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> minorityPriests = minorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> allPriests = new HashMap<>();
    allPriests.putAll(majorityPriests);
    allPriests.putAll(minorityPriests);

    minorityPriests.values().forEach(p -> p.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(allPriests.values(), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    majorityPriests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMajorityDoublePropose() throws Exception {
    final Stream<TestKit> majorityKits = Stream.generate(() -> new TestKit(system)).limit(PRIESTS_COUNT - MINORITY);
    final Stream<TestKit> minorityKits = Stream.generate(() -> new TestKit(system)).limit(MINORITY);

    final Map<TestKit, ActorRef> majorityPriests = majorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> minorityPriests = minorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> allPriests = new HashMap<>();
    allPriests.putAll(majorityPriests);
    allPriests.putAll(minorityPriests);

    final ActorRef leader = system.actorOf(DecreePresident.props(allPriests.values(), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    allPriests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));

    minorityPriests.values().forEach(p -> p.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef anotherLeader = system.actorOf(DecreePresident.props(allPriests.values(), 1));
    anotherLeader.tell(new PaxosAPI.Propose<>("ANOTHER_VALUE", 1), ActorRef.noSender());
    majorityPriests.keySet().forEach(kit -> kit.expectMsg(new PaxosAPI.Decide<>("VALUE", 1)));
  }

  @Test
  public void testMinorityPropose() throws Exception {
    final Stream<TestKit> majorityKits = Stream.generate(() -> new TestKit(system)).limit(PRIESTS_COUNT - MINORITY);
    final Stream<TestKit> minorityKits = Stream.generate(() -> new TestKit(system)).limit(MINORITY);

    final Map<TestKit, ActorRef> majorityPriests = majorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> minorityPriests = minorityKits.collect(Collectors.toMap(
            Function.identity(),
            kit -> system.actorOf(DecreePriest.props(1, kit.getRef()))
    ));

    final Map<TestKit, ActorRef> allPriests = new HashMap<>();
    allPriests.putAll(majorityPriests);
    allPriests.putAll(minorityPriests);

    majorityPriests.values().forEach(p -> p.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(allPriests.values(), 1));
    leader.tell(new PaxosAPI.Propose<>("VALUE", 1), ActorRef.noSender());
    allPriests.keySet().forEach(testKit -> testKit.expectNoMsg(Duration.create(30, MILLISECONDS)));
  }
}