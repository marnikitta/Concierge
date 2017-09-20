package marnikitta.concierge.common;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.typesafe.config.ConfigFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

public abstract class ConciergeTest {
  protected ActorSystem system;

  @BeforeSuite
  public void initSystem() {
    system = ActorSystem.create("concierge-test", ConfigFactory.load());
  }

  @AfterSuite
  public void deinitSystem() {
    TestKit.shutdownActorSystem(system);
  }
}
