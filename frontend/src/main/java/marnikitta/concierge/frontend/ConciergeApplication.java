package marnikitta.concierge.frontend;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import marnikitta.concierge.common.Cluster;
import marnikitta.concierge.kv.LinearizableStorage;
import marnikitta.concierge.kv.session.SessionAPI;
import marnikitta.concierge.kv.storage.StorageAPI;
import marnikitta.concierge.model.ConciergeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Function1;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static akka.http.javadsl.server.PathMatchers.longSegment;
import static akka.pattern.PatternsCS.ask;
import static com.typesafe.config.ConfigFactory.parseString;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;
import static scala.compat.java8.JFunction.func;

public final class ConciergeApplication extends AllDirectives {
  private static final Logger LOG = LoggerFactory.getLogger(ConciergeApplication.class);
  private final ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule());

  private final ConciergeConfig config;

  private ActorRef kv;

  public ConciergeApplication(ConciergeConfig config) {
    this.config = config;
  }

  public static void main(String... args) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]))) {
      final ObjectMapper mapper = new ObjectMapper();
      final ConciergeConfig conciergeConfig = mapper.readValue(reader, ConciergeConfig.class);
      new ConciergeApplication(conciergeConfig).run();
    }
  }

  public void run() {
    final Config conf = parseString("akka.remote.netty.tcp.hostname=" + config.hostname())
            .withFallback(parseString("akka.remote.netty.tcp.port=" + config.actorPort()))
            .withFallback(ConfigFactory.load("remote"));

    final ActorSystem system = ActorSystem.create("concierge", conf);
    kv = system.actorOf(LinearizableStorage.props(new Cluster(config.cluster().paths(), "kv")), "kv");

    final ActorMaterializer materializer = ActorMaterializer.create(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> theFlow = createRoute().flow(system, materializer);

    final ConnectHttp host = ConnectHttp.toHost(config.hostname(), config.clientPort());
    Http.get(system).bindAndHandle(theFlow, host, materializer);

    LOG.info("Ama up");
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

    final int timeout = 500;
    final Route sessionPath = route(
            post(() ->
                    pathEnd(() ->
                            onComplete(
                                    ask(kv, new SessionAPI.CreateSession(), timeout),
                                    sessionTry -> sessionTry.map(expectedFailureMapper).get()
                            )
                    )
            ),
            patch(() ->
                    path(longSegment(), sessionId ->
                            onComplete(
                                    ask(kv, new SessionAPI.Heartbeat(sessionId), timeout),
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
                                            ), timeout),
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
                                            ), timeout),
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
                                            ), timeout),
                                            sessionTry -> sessionTry.map(expectedFailureMapper).get()
                                    )
                            )
                    )
            ),
            delete(() ->
                    path(key ->
                            parameterMap(params ->
                                    onComplete(
                                            ask(kv, new StorageAPI.Delete(
                                                    key,
                                                    parseLong(params.get("version")),
                                                    parseLong(params.get("session"))
                                            ), timeout),
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
