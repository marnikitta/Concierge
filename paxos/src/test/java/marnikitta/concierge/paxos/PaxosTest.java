package marnikitta.concierge.paxos;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.common.ConciergeTest;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PaxosTest extends ConciergeTest {
  public static final int PRIESTS_COUNT = 17;
  public static final int MINORITY = PRIESTS_COUNT / 2 - 1;

  @Test
  public void testSimplePropose() throws Exception {
    final List<TestPriest> testPriests = Stream.generate(this::testPriest)
            .limit(PRIESTS_COUNT)
            .collect(toList());

    final Set<ActorPath> priestsPaths = testPriests.stream().map(p -> p.path).collect(toSet());

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose("VALUE", 1), ActorRef.noSender());

    final List<TestKit> kits = testPriests.stream().map(p -> p.kit).collect(toList());

    kits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide("VALUE", 1)));
  }

  @Test
  public void testMajorityPropose() throws Exception {
    final List<TestPriest> majorityTestPriests = Stream.generate(this::testPriest)
            .limit(PRIESTS_COUNT - MINORITY)
            .collect(toList());

    final List<TestPriest> minorityTestPriests = Stream.generate(this::testPriest)
            .limit(MINORITY)
            .collect(toList());

    final Set<ActorPath> priestsPaths = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .map(p -> p.path)
            .collect(toSet());

    final List<TestKit> majorityKits = majorityTestPriests.stream().map(p -> p.kit).collect(toList());

    minorityTestPriests.forEach(p -> p.priest.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose("VALUE", 1), ActorRef.noSender());
    majorityKits.forEach(kit -> kit.expectMsg(new PaxosAPI.Decide("VALUE", 1)));
  }

  @Test
  public void testMinorityPropose() throws Exception {
    final List<TestPriest> majorityTestPriests = Stream.generate(this::testPriest)
            .limit(PRIESTS_COUNT - MINORITY)
            .collect(toList());

    final List<TestPriest> minorityTestPriests = Stream.generate(this::testPriest)
            .limit(MINORITY)
            .collect(toList());

    final Set<ActorPath> priestsPaths = Stream
            .concat(majorityTestPriests.stream(), minorityTestPriests.stream())
            .map(p -> p.path)
            .collect(toSet());

    final List<TestKit> majorityKits = majorityTestPriests.stream().map(p -> p.kit).collect(toList());

    majorityTestPriests.forEach(p -> p.priest.tell(PoisonPill.getInstance(), ActorRef.noSender()));

    final ActorRef leader = system.actorOf(DecreePresident.props(new Cluster(priestsPaths), 1));
    leader.tell(new PaxosAPI.Propose("VALUE", 1), ActorRef.noSender());
    majorityKits.forEach(kit -> kit.expectNoMsg(Duration.create(1, SECONDS)));
  }

  private TestPriest testPriest() {
    final TestKit kit = new TestKit(system);
    final String suffix = UUID.randomUUID().toString();
    return new TestPriest(
            system.actorOf(DecreePriest.props(1, kit.getRef()), suffix),
            system.child(suffix),
            kit
    );
  }

  private static class TestPriest {
    public final ActorRef priest;
    public final ActorPath path;
    public final TestKit kit;

    public TestPriest(ActorRef priest, ActorPath path, TestKit kit) {
      this.priest = priest;
      this.path = path;
      this.kit = kit;
    }
  }
}