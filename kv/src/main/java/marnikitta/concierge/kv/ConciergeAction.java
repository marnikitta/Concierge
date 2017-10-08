package marnikitta.concierge.kv;

import marnikitta.concierge.kv.session.SessionManager;
import marnikitta.concierge.kv.storage.Storage;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public interface ConciergeAction {
  @Nullable
  Object apply(Storage storage, SessionManager manager, Instant timestamp);
}
