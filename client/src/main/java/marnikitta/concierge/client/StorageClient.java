package marnikitta.concierge.client;

import marnikitta.concierge.model.StorageEntry;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface StorageClient {
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
  Call<Boolean> delete(@Path("key") String key,
                       @Query("session") long session,
                       @Query("version") long version);
}
