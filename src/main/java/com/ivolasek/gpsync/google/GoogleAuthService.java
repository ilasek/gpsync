package com.ivolasek.gpsync.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

/**
 * Initiate flow:
 * response.sendRedirect(googleAuthService.getAuthorizationUrl());
 *
 * On the redirect url, fetch the authorization code:
 * {@link PhotosLibraryClient} client = googleAuthService.retrieveClientSettings(code)
 */
@Service
@Slf4j
public class GoogleAuthService {

    public static final String REDIRECT_URI = "http://localhost:8080";

    private GoogleAuthorizationCodeFlow flow;

    private GoogleClientSecrets getClientSecrets() throws IOException {
        return GoogleClientSecrets.load(new JacksonFactory(),
                new InputStreamReader(GoogleAuthService.class.getResourceAsStream("/client_id.json")));
    }

    private String requestAccessToken(String code) throws IOException {
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleTokenResponse response =
                new GoogleAuthorizationCodeTokenRequest(new NetHttpTransport(), jsonFactory,
                        getClientSecrets().getDetails().getClientId(), getClientSecrets().getDetails().getClientSecret(),
                        code, REDIRECT_URI)
                        .execute();

        return response.getAccessToken();
    }

    /**
     *
     * @return Authorization URL to redirect the user to in order to initiate the OAuth2 flow.
     * @throws IOException When there are problems reading credentials.
     */
    String getAuthorizationUrl() throws IOException {
        if (flow == null) {
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(), jsonFactory, getClientSecrets(),
                    Collections.singleton("https://www.googleapis.com/auth/photoslibrary.readonly")).setDataStoreFactory(
                    new MemoryDataStoreFactory()).build();
        }

        return flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URI)
                .toString();
    }

    /**
     * Sets the credentials after a successful retrieval of the authorization grant code. Call the getAuthorizationUrl
     * method first and then after the redirect use this method to initialize credentials.
     * @param code Authorization Grant Code.
     * @throws GoogleAuthException When there is a problem with using the authorization grant to query Photos services.
     */
    PhotosLibrarySettings retrieveClientSettings(String code) throws GoogleAuthException {
        try {
            GoogleCredentials credentials = GoogleCredentials.create(
                    new AccessToken(requestAccessToken(code), null));

            return PhotosLibrarySettings.newBuilder()
                            .setCredentialsProvider(
                                    FixedCredentialsProvider.create(credentials))
                            .build();
        } catch (IOException e) {
            throw new GoogleAuthException("Problem getting authFlow", e);
        }
    }
}
