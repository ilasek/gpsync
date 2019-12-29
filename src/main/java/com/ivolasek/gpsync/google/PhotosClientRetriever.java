package com.ivolasek.gpsync.google;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.PhotosLibrarySettings;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Helper class to hold OAuth2 settings used to  retrieve an appropriately initialized {@link PhotosLibraryClient}.
 */
@Setter
@Slf4j
public class PhotosClientRetriever {

    private PhotosLibrarySettings settings;

    public PhotosLibraryClient getClient() throws GoogleAuthException {
        if (settings == null) {
            throw new IllegalStateException("PhotosLibraryClient not initialized yet. You have to go through the " +
                    "authentication flow first.");
        } else {
            try {
                return PhotosLibraryClient.initialize(settings);
            } catch (IOException e) {
                log.error("Problem providing settings for client. Provided settings {}", settings);
                throw new GoogleAuthException("Problem initializing client using provided settings", e);
            }
        }
    }

}
