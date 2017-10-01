package concierge.kv;

import concierge.kv.session.SessionManager;
import concierge.kv.storage.Storage;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public interface ConciergeAction {
  @Nullable
  Object doIt(Storage storage, SessionManager manager, Instant timestamp) throws ConciergeActionException;
}
