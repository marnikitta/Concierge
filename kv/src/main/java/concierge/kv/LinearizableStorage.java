package concierge.kv;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import concierge.kv.session.SessionManager;
import concierge.kv.storage.Storage;
import marnikitta.concierge.atomic.AtomicBroadcast;
import marnikitta.concierge.atomic.AtomicBroadcastAPI;
import marnikitta.concierge.common.Cluster;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class LinearizableStorage extends AbstractActor {
  private final LoggingAdapter LOG = Logging.getLogger(this);
  private final ActorRef atomicBroadcast;
  private final Map<UUID, ActorRef> inFlight = new HashMap<>();

  private final Storage storage = new Storage();
  private final SessionManager sessionManager = new SessionManager();

  private LinearizableStorage(Cluster cluster) {
    this.atomicBroadcast = context().actorOf(
            AtomicBroadcast.props(self(), new Cluster(cluster.paths(), "atomic")),
            "atomic"
    );
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(BroadcastEntry.class, this::onBroadcastEntry)
            .match(ConciergeAction.class, this::onAction)
            .build();
  }

  private void onAction(ConciergeAction action) {
    final BroadcastEntry broadcastEntry = new BroadcastEntry(action);
    atomicBroadcast.tell(new AtomicBroadcastAPI.Broadcast(broadcastEntry), self());
    inFlight.put(broadcastEntry.broadcastUUID(), sender());
  }

  private void onBroadcastEntry(BroadcastEntry entry) {
    try {
      @Nullable final Object result = entry.action().doIt(storage, sessionManager);
      if (inFlight.containsKey(entry.broadcastUUID()) && result != null) {
        inFlight.get(entry.broadcastUUID()).tell(result, self());
      }
    } catch (ConciergeActionException e) {
      if (inFlight.containsKey(entry.broadcastUUID())) {
        inFlight.get(entry.broadcastUUID()).tell(e, self());
      }
    }
  }

  private static final class BroadcastEntry {
    private final ConciergeAction action;
    private final UUID broadcastUUID;

    private BroadcastEntry(ConciergeAction action) {
      this.action = action;
      this.broadcastUUID = UUID.randomUUID();
    }

    public ConciergeAction action() {
      return action;
    }

    public UUID broadcastUUID() {
      return broadcastUUID;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final BroadcastEntry that = (BroadcastEntry) o;
      return Objects.equals(broadcastUUID, that.broadcastUUID);
    }

    @Override
    public int hashCode() {
      return Objects.hash(broadcastUUID);
    }
  }
}

