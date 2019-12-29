package com.ivolasek.gpsync.google;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.google.protobuf.util.Timestamps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class PhotosService {
    private final PhotosClientRetriever photosClientRetriever;

    private GoogleAuthService googleAuthService;

    @Autowired
    public PhotosService(GoogleAuthService googleAuthService, PhotosClientRetriever photosClientRetriever) {
        this.googleAuthService = googleAuthService;
        this.photosClientRetriever = photosClientRetriever;
    }

    /**
     *
     * @return Authorization URL to redirect the user to in order to initiate the OAuth2 flow.
     * @throws IOException When there are problems reading credentials.
     */
    public String getAuthorizationUrl() throws IOException {
        return googleAuthService.getAuthorizationUrl();
    }

    /**
     * Sets the credentials after a successful retrieval of the authorization grant code. Call the getAuthorizationUrl
     * method first and then after the redirect use this method to initialize credentials.
     * @param code Authorization Grant Code.
     * @throws GoogleAuthException When there is a problem with using the authorization grant to query Photos services.
     */
    public void setAuthGrantCode(String code) throws GoogleAuthException {
        photosClientRetriever.setSettings(googleAuthService.retrieveClientSettings(code));
    }

    /**
     * Retrieves all albums from a given Google Photos profile.
     * @param client Client retrieved through {@link PhotosClientRetriever}
     * @return List of all Google Photos albums.
     */
    public List<Album> getAlbums(PhotosLibraryClient client) {
        return StreamSupport.stream(client.listAlbums().iterateAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     *
     * @param client Client retrieved through {@link PhotosClientRetriever}
     * @param albumId Google Photos album ID.
     * @param date The date used to filter Media Items - only Items created after that date are retrieved.
     * @return List of Media Items from a Google Photos album that were created after a given date.
     */
    public List<MediaItem> getMediaCreatedAfter(PhotosLibraryClient client, String albumId, Date date) {
        return StreamSupport.stream(client.searchMediaItems(albumId).iterateAll().spliterator(), false)
                .filter(mediaItem -> Timestamps.toMillis(mediaItem.getMediaMetadata().getCreationTime()) > date.getTime())
                .collect(Collectors.toList());
    }
}
