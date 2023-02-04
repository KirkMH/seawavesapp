package com.asu.seawavesapp.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * <code>ApiClient</code> class uses Retrofit to link to the web server's API endpoints.
 * It implements a singleton design strategy so that only one instance of RestApi will be created.
 */
public class ApiClient {
    private static final String BASE_URL = "https://seawavesserver.pythonanywhere.com/api/";
    private static RestApi api = null;

    /**
     * Returns an instance of the API client.
     *
     * @return API client
     */
    public static RestApi getApi() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(RestApi.class);
        }
        return api;
    }
}
