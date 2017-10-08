package marnikitta.concierge.frontend;

import akka.NotUsed;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.RootActorPath;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.directives.RouteAdapter;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.ConfigFactory;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.kv.LinearizableStorage;
import marnikitta.concierge.kv.session.SessionAPI;
import marnikitta.concierge.kv.storage.StorageAPI;
import marnikitta.concierge.model.ConciergeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function1;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.PathMatchers.longSegment;
import static akka.pattern.PatternsCS.ask;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static scala.compat.java8.JFunction.func;

public final class ConciergeApplication extends AllDirectives {
  private final Logger LOG = LoggerFactory.getLogger(ConciergeApplication.class);
  private final ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule());

  private final Cluster cluster;

  private ActorRef kv;

  public ConciergeApplication(Cluster cluster) {
    this.cluster = cluster;
  }

  public static void main(String... args) throws IOException {
    final Set<ActorPath> clusterPaths = new HashSet<>();

    final List<String> localString = Collections.singletonList("localhost:23456");

    for (String arg : localString) {
      final String[] split = arg.split(":");
      final InetAddress host = InetAddress.getByName(split[0]);
      final int port = Integer.parseInt(split[1]);

      final Address actorSystemAddress = new Address("akka.tcp", "concierge", host.getHostName(), port);
      clusterPaths.add(RootActorPath.apply(actorSystemAddress, "/").child("user"));
    }

    new ConciergeApplication(new Cluster(clusterPaths)).run();
  }

  public void run() throws IOException {
    final ConnectHttp host = ConnectHttp.toHost("localhost", 8080);

    final ActorSystem system = ActorSystem.create("concierge", ConfigFactory.load("remote"));
    kv = system.actorOf(LinearizableStorage.props(new Cluster(cluster.paths(), "kv")), "kv");

    final ActorMaterializer materializer = ActorMaterializer.create(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> theFlow = createRoute().flow(system, materializer);
    final CompletionStage<ServerBinding> binding = Http.get(system).bindAndHandle(theFlow, host, materializer);

    LOG.info("Ama up");
    System.in.read();

    binding.thenCompose(ServerBinding::unbind)
            .whenComplete((unbound, e) -> system.terminate());
  }

  private Route createRoute() {
    final Function1<Object, RouteAdapter> expectedFailureMapper = func(response -> {
      if (response instanceof ConciergeException) {
        return complete(
                StatusCodes.IM_A_TEAPOT,
                response,
                Jackson.marshaller(mapper)
        );
      } else {
        return complete(StatusCodes.OK, response, Jackson.marshaller(mapper));
      }
    });

    final Route sessionPath = route(
            post(() ->
                    pathEnd(() ->
                            onComplete(
                                    ask(kv, new SessionAPI.CreateSession(), 100),
                                    sessionTry -> sessionTry.map(expectedFailureMapper).get()
                            )
                    )
            ),
            patch(() ->
                    path(longSegment(), sessionId ->
                            onComplete(
                                    ask(kv, new SessionAPI.Heartbeat(sessionId), 100),
                                    sessionTry -> sessionTry.map(expectedFailureMapper).get()
                            )
                    )
            )
    );

    final Route storagePath = route(
            put(() ->
                    path(key ->
                            parameterMap(params ->
                                    onComplete(
                                            ask(kv, new StorageAPI.Create(
                                                    key,
                                                    params.get("value"),
                                                    parseLong(params.get("session")),
                                                    parseBoolean(params.getOrDefault("ephemeral", "false"))
                                            ), 100),
                                            sessionTry -> sessionTry.map(expectedFailureMapper).get()
                                    )
                            )
                    )
            ),
            patch(() ->
                    path(key ->
                            parameterMap(params ->
                                    onComplete(
                                            ask(kv, new StorageAPI.Update(
                                                    key,
                                                    parseLong(params.get("version")),
                                                    params.get("value"),
                                                    parseLong(params.get("session"))
                                            ), 100),
                                            sessionTry -> sessionTry.map(expectedFailureMapper).get()
                                    )
                            )
                    )
            ),
            get(() ->
                    path(key ->
                            parameter("session", session ->
                                    onComplete(
                                            ask(kv, new StorageAPI.Read(
                                                    key,
                                                    parseLong(session)
                                            ), 100),
                                            sessionTry -> sessionTry.map(expectedFailureMapper).get()
                                    )
                            )
                    )
            )
    );

    return route(
            pathPrefix("sessions", () -> sessionPath),
            pathPrefix("keys", () -> storagePath),
            path("ping", () -> complete("pong"))
    );
  }
}
