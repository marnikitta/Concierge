package marnikitta.concierge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import marnikitta.concierge.model.Session;
import marnikitta.concierge.model.StorageEntry;
import org.jetbrains.annotations.Nullable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class Concierge implements AutoCloseable {
  private final ObjectMapper mapper = new ObjectMapper()
          .registerModule(new JavaTimeModule());

  private final Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("http://localhost:8080")
          .addConverterFactory(JacksonConverterFactory.create(mapper))
          .build();

  private final StorageClient storageClient = retrofit.create(StorageClient.class);
  private final SessionClient sessionClient = retrofit.create(SessionClient.class);

  @Nullable
  private Thread pinger;

  @Nullable
  private volatile Session session = null;

  public long createSession() throws IOException {
    final Response<Session> execute = sessionClient.createSession(Math.abs(new Random().nextLong())).execute();
    session = execute.body();
    System.out.println(execute);

    if (pinger != null) {
      pinger.interrupt();
      try {
        pinger.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    pinger = new Thread(() -> {
      while (true) {
        try {
          sessionClient.heartbeat(session.id()).execute();
          TimeUnit.MILLISECONDS.sleep(session.heartbeatDelay().toMillis() / 2);
        } catch (InterruptedException ignored) {
          return;
        } catch (IOException e) {
          return;
        }
      }
    });
    pinger.start();

    return session.id();
  }

  @Override
  public void close() throws Exception {
    if (pinger != null) {
      pinger.interrupt();
      pinger.join();
    }
  }

  public static void main(String... args) throws Exception {
    try (Concierge concierge = new Concierge()) {
      concierge.createSession();
      TimeUnit.HOURS.sleep(1);
    }
  }
}

interface StorageClient {

  @PUT("{sessionId}/{key}")
  Call<StorageEntry> create(@Path("sessionId") long sessionId,
                            @Path("key") String key,
                            @Query("value") String value,
                            @Query("ephemeral") boolean ephemeral);

  @GET("{sessionId}/{key}")
  Call<StorageEntry> get(@Path("sessionId") long sessionId, @Path("key") String key);

  @POST("{sessionId}/{key}")
  Call<StorageEntry> update(@Path("sessionId") long sessionId,
                            @Path("key") String key,
                            @Query("value") String value,
                            @Query("expectedVersion") long version);

  @DELETE("{sessionId}/{key}")
  Call<StorageEntry> delete(@Path("sessionId") long sessionId,
                            @Path("key") String key);
}

interface SessionClient {
  @PUT("{sessionId}")
  Call<Session> createSession(@Path("sessionId") long id);

  @PATCH("{sessionId}")
  Call<Session> heartbeat(@Path("sessionId") long id);
}
