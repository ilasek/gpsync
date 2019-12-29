package com.ivolasek.gpsync.filesystem;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "filesystem")
@Data
public class FileSystemConfig {
    private String sourceFolder;
    private String targetFolder;
    private int stopAfterNEmptyAlbums;
}
