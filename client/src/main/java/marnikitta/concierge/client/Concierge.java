package marnikitta.concierge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import marnikitta.concierge.model.ConciergeException;
import marnikitta.concierge.model.Session;
import marnikitta.concierge.model.StorageEntry;
import marnikitta.concierge.model.session.NoSuchSessionException;
import marnikitta.concierge.model.session.SessionExpiredException;
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
import java.util.concurrent.TimeUnit;

public final class Concierge implements AutoCloseable {
  public static final int I_AM_TEAPOT = 418;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private final Thread pinger;
  private final Session session;

  private final SessionClient sessionClient;
  private final StorageClient storageClient;

  public Concierge(String host, int port) throws IOException {
    final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .baseUrl("http://" + host + ':' + port)
            .build();

    sessionClient = retrofit.create(SessionClient.class);
    storageClient = retrofit.create(StorageClient.class);

    final Response<Session> response = sessionClient.createSession().execute();
    if (response.isSuccessful() && response.body() != null) {
      session = response.body();
    } else {
      throw new IOException(response.errorBody().string());
    }

    pinger = new Thread(() -> {
      while (true) {
        try {
          sessionClient.heartbeat(session.id()).execute();
          TimeUnit.MILLISECONDS.sleep(session.heartbeatDelay().toMillis() / 2);
        } catch (SessionExpiredException | InterruptedException | NoSuchSessionException e) {
          return;
        } catch (IOException ignored) {
        }
      }
    });

    pinger.setDaemon(true);
    pinger.start();
  }

  public StorageEntry create(String key, String value) throws IOException {
    final Response<StorageEntry> execute = storageClient.create(key, session.id(), value, false).execute();
    return vauleOrException(execute);
  }

  public StorageEntry createEphemeral(String key, String value) throws IOException {
    final Response<StorageEntry> execute = storageClient.create(key, session.id(), value, true).execute();
    return vauleOrException(execute);
  }

  public StorageEntry get(String key) throws IOException {
    final Response<StorageEntry> execute = storageClient.get(key, session.id()).execute();
    return vauleOrException(execute);
  }

  public StorageEntry update(String key, String value, long version) throws IOException {
    final Response<StorageEntry> execute = storageClient.update(key, session.id(), value, version).execute();
    return vauleOrException(execute);
  }

  public StorageEntry delete(String key, long version) throws IOException {
    final Response<StorageEntry> execute = storageClient.delete(key, session.id(), version).execute();
    return vauleOrException(execute);
  }

  private <T> T vauleOrException(Response<T> execute) throws IOException {
    if (execute.isSuccessful()) {
      return execute.body();
    } else if (execute.code() == I_AM_TEAPOT) {
      throw mapper.readValue(
              execute.errorBody().bytes(),
              ConciergeException.class
      );
    } else {
      throw new IOException(execute.errorBody().string());
    }
  }

  @Override
  public void close() throws Exception {
    pinger.interrupt();
    pinger.join();
  }

  public static void main(String... args) throws Exception {
    try (Concierge concierge = new Concierge("localhost", 8080)) {
      StorageEntry prev = concierge.create("aba", "caba");

      System.out.println(prev);

      for (int i = 0; i < 10000; ++i) {
        prev = concierge.update(
                prev.key(),
                String.valueOf(i),
                prev.version()
        );

        System.out.println(prev);
      }
    }
  }

  private interface StorageClient {
    @PUT("keys/{key}")
    Call<StorageEntry> create(@Path("key") String key,
                              @Query("session") long session,
                              @Query("value") String value,
                              @Query("ephemeral") boolean ephemeral);

    @GET("keys/{key}")
    Call<StorageEntry> get(@Path("key") String key,
                           @Query("session") long session);

    @PATCH("keys/{key}")
    Call<StorageEntry> update(@Path("key") String key,
                              @Query("session") long session,
                              @Query("value") String value,
                              @Query("version") long version);

    @DELETE("keys/{key}")
    Call<StorageEntry> delete(@Path("key") String key,
                              @Query("session") long session,
                              @Query("version") long version);
  }

  private interface SessionClient {
    @POST("sessions")
    Call<Session> createSession();

    @PATCH("sessions/{session}")
    Call<Session> heartbeat(@Path("session") long id);
  }
}
