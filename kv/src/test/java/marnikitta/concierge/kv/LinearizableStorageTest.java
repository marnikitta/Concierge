package marnikitta.concierge.kv;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.testkit.javadsl.TestKit;
import marnikitta.concierge.model.Session;
import marnikitta.concierge.kv.session.SessionAPI;
import marnikitta.concierge.kv.storage.StorageAPI;
import marnikitta.concierge.model.StorageEntry;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.common.ConciergeTest;
import org.testng.annotations.Test;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class LinearizableStorageTest extends ConciergeTest {
  private static final int PRIEST_COUNT = 17;

  @Test
  public void createSessionTest() {
    final ActorRef storage = storage("createSessionTest");
    final TestKit kit = new TestKit(system);

    kit.send(storage, new SessionAPI.CreateSession());
    final Object o = kit.receiveOne(Duration.create(3, TimeUnit.SECONDS));
    assertTrue(o instanceof Session);
  }

  @Test
  public void readWriteTest() {
    final ActorRef storage = storage("readWriteTest");
    final TestKit kit = new TestKit(system);

    kit.send(storage, new SessionAPI.CreateSession());
    final Session session = (Session) kit.receiveOne(Duration.create(3, TimeUnit.SECONDS));

    kit.send(storage, new StorageAPI.Create("key", "value", session.id(), false));
    final Object o = kit.receiveOne(Duration.create(3, TimeUnit.SECONDS));

    assertTrue(o instanceof StorageEntry);
  }

  private ActorRef storage(String prefix) {
    final Set<ActorPath> storagePaths = LongStream.range(0, PRIEST_COUNT)
            .boxed()
            .map(l -> system.child(prefix + l))
            .collect(toSet());

    final List<ActorRef> testPriests = LongStream.range(0, PRIEST_COUNT)
            .boxed()
            .map(l -> system.actorOf(LinearizableStorage.props(new Cluster(storagePaths)), prefix + l))
            .collect(toList());

    return testPriests.get(0);
  }
}