package marnikitta.concierge.client;

import marnikitta.concierge.model.Session;
import retrofit2.Call;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

interface SessionClient {
  @POST("sessions")
  Call<Session> createSession();

  @PATCH("sessions/{session}")
  Call<Session> heartbeat(@Path("session") long id);
}
