package marnikitta.concierge.frontend;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletionStage;

public final class ConciergeApplication extends AllDirectives {
  public ConciergeApplication() {
  }

  public static void main(String... args) throws IOException {
    new ConciergeApplication().run();
  }

  public void run() throws IOException {
    final ConnectHttp host = ConnectHttp.toHost("localhost", 8080);

    final ActorSystem system = ActorSystem.create();
    final ActorMaterializer materializer = ActorMaterializer.create(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> theFlow = createRoute().flow(system, materializer);
    final CompletionStage<ServerBinding> binding = Http.get(system)
            .bindAndHandle(theFlow, host, materializer);

    System.out.println("Ama up");
    System.in.read();

    binding.thenCompose(ServerBinding::unbind)
            .whenComplete((unbound, e) -> system.terminate());
  }

  public Route createRoute() {
    return route();
  }
}
