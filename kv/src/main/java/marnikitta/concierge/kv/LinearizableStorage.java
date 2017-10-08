package marnikitta.concierge.kv;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import marnikitta.concierge.atomic.AtomicBroadcast;
import marnikitta.concierge.atomic.AtomicBroadcastAPI;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.kv.session.SessionManager;
import marnikitta.concierge.kv.storage.Storage;
import marnikitta.concierge.model.ConciergeException;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
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

  public static Props props(Cluster cluster) {
    return Props.create(LinearizableStorage.class, cluster);
  }

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(AtomicBroadcastAPI.Deliver.class, d -> onBroadcastEntry((BroadcastEntry) d.value()))
            .match(ConciergeAction.class, this::onAction)
            .build();
  }

  private void onAction(ConciergeAction action) {
    final BroadcastEntry broadcastEntry = new BroadcastEntry(action, Instant.now());
    atomicBroadcast.tell(new AtomicBroadcastAPI.Broadcast(broadcastEntry), self());
    inFlight.put(broadcastEntry.broadcastUUID(), sender());
  }

  private void onBroadcastEntry(BroadcastEntry entry) {
    LOG.info("Broadcast received: {}", entry);

    try {
      @Nullable final Object result = entry.action().apply(storage, sessionManager, entry.broadcastedAt());
      if (inFlight.containsKey(entry.broadcastUUID()) && result != null) {
        inFlight.get(entry.broadcastUUID()).tell(result, self());
        inFlight.remove(entry.broadcastUUID());
      }
    } catch (ConciergeException e) {
      LOG.info("ConciergeException: {}", e);
      if (inFlight.containsKey(entry.broadcastUUID())) {
        inFlight.get(entry.broadcastUUID()).tell(e, self());
        inFlight.remove(entry.broadcastUUID());
      }
    }
  }

  private static final class BroadcastEntry {
    private final Instant broadcastedAt;
    private final ConciergeAction action;
    private final UUID broadcastUUID;

    private BroadcastEntry(ConciergeAction action, Instant broadcastedAt) {
      this.action = action;
      this.broadcastedAt = broadcastedAt;
      this.broadcastUUID = UUID.randomUUID();
    }

    public Instant broadcastedAt() {
      return broadcastedAt;
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

    @Override
    public String toString() {
      return "BroadcastEntry{" +
              "broadcastedAt=" + broadcastedAt +
              ", action=" + action +
              ", broadcastUUID=" + broadcastUUID +
              '}';
    }
  }
}

