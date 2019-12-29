package com.ivolasek.gpsync.google;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class PhotosConfig {
    @Bean
    @SessionScope
    public PhotosClientRetriever getPhotosClientRetriever() {
        return new PhotosClientRetriever();
    }
}
