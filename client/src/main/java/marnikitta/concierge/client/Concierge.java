package marnikitta.concierge.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import marnikitta.concierge.model.ConciergeException;
import marnikitta.concierge.model.Session;
import marnikitta.concierge.model.StorageEntry;
import marnikitta.concierge.model.session.NoSuchSessionException;
import marnikitta.concierge.model.session.SessionExpiredException;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.unmodifiableList;

public final class Concierge implements AutoCloseable {
  public static final int I_AM_TEAPOT = 418;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private final Thread pinger;
  private final Session session;

  private final SessionClient sessionClient;
  private final StorageClient storageClient;

  public Concierge(List<String> hosts) throws IOException {
    final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new HostSelectorInterceptor(hosts))
            .build();

    final Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .baseUrl("http://" + hosts.get(0) + "/")
            .callFactory(client)
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
          TimeUnit.MILLISECONDS.sleep(session.heartbeatDelay().toMillis() / 4);
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

  public boolean delete(String key, long version) throws IOException {
    final Response<Boolean> execute = storageClient.delete(key, session.id(), version).execute();
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

  private static final class HostSelectorInterceptor implements Interceptor {
    private final List<String> hosts;

    private volatile int currentHostId = 0;

    public HostSelectorInterceptor(List<String> hosts) {
      this.hosts = unmodifiableList(hosts);
    }

    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
      final String[] currentString = hosts.get(currentHostId).split(":");
      final String currentHost = currentString[0];
      final int currentPort = Integer.parseInt(currentString[1]);

      final HttpUrl url = chain.request().url().newBuilder().host(currentHost).port(currentPort).build();
      final Request request = chain.request().newBuilder().url(url).build();

      try {
        final okhttp3.Response response = chain.proceed(request);
        if (response.code() >= 500 && response.code() < 600) {
          changeHost();
        }
        return response;
      } catch (IOException e) {
        changeHost();
        throw e;
      }
    }

    private void changeHost() {
      currentHostId = (currentHostId + 1) % hosts.size();
    }
  }
}
