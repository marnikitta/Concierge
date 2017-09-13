package marnikitta.concierge.atomic;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AtomicBroadcast extends AbstractActor {
  private final Set<ActorRef> remoteBroadcasts = new HashSet<>();
  private final Map<ActorRef, ActorRef> electorToBroadcast = new HashMap<>();

  @Override
  public Receive createReceive() {
    return ReceiveBuilder.create()
            .match(AtomicBroadcastAPI.RegisterBroadcasts.class, registerBroadcasts -> {
              remoteBroadcasts.addAll(registerBroadcasts.broadcasts);
              remoteBroadcasts.forEach(b -> b.tell(new BroadcastMessages.IdentifyElector(), self()));
              getContext().become(identifyingElectors());
            }).build();
  }

  private Receive identifyingElectors() {
    return ReceiveBuilder.create()
            .match(BroadcastMessages.ElectorIdentity.class, identity -> {
                      electorToBroadcast.put(identity.elector, sender());
                      if (electorToBroadcast.values().containsAll(remoteBroadcasts)) {
                        getContext().become();
                      }
                    }
            ).build();
  }
}
