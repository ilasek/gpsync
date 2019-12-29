package com.ivolasek.gpsync.filesystem;

import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.types.proto.Album;
import com.google.photos.types.proto.MediaItem;
import com.google.protobuf.util.Timestamps;
import com.ivolasek.gpsync.google.GoogleAuthException;
import com.ivolasek.gpsync.google.PhotosClientRetriever;
import com.ivolasek.gpsync.google.PhotosService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FileWalkerService {
    private static final SimpleDateFormat PREFIX_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_dd_");

    private static final SimpleDateFormat MONTH_PREFIX_DATE_FORMAT = new SimpleDateFormat("yyyy_MM_");

    private static final String REMAINING_POSTFIX = "01_Zbytek";

    private static final String INVALID_PREFIX = "INVALID_PREFIX_";

    @Autowired
    private PhotosService photosService;

    @Autowired
    private PhotosClientRetriever photosClientRetriever;

    @Autowired
    private FileSystemConfig fileSystemConfig;

    public void walkFiles() throws IOException {
        Date oldestFileDate = getOldestFileDate();
        int emptyAlbumsCounter = 0;
        try (PhotosLibraryClient client = photosClientRetriever.getClient()) {
            List<Album> albums = photosService.getAlbums(client);
            log.info("Walking through albums");
            for (Album album : albums) {
                log.info("Walking through album {}", album.getTitle());
                List<MediaItem> items = photosService.getMediaCreatedAfter(client, album.getId(), oldestFileDate);
                log.info("Album {} contains {} mediaItems after the date", album.getTitle(), items.size());
                moveFilesAccordingly(items, album.getTitle());
                emptyAlbumsCounter = (items.size() > 0) ? 0 : emptyAlbumsCounter + 1;
                if (emptyAlbumsCounter > fileSystemConfig.getStopAfterNEmptyAlbums()) {
                    log.info("Stopping after {} empty albums", emptyAlbumsCounter);
                    break;
                }
            }
            log.info("Processing remaining files");
            processRemainingFiles();
            log.info("Done processing remaining files");
        } catch (GoogleAuthException e) {
            log.error("Unable to retrieve Google Photos Albums", e);
        }
    }

    private void processRemainingFiles() {
        File sourceDir = new File(fileSystemConfig.getSourceFolder());
        File[] sourceFiles = sourceDir.listFiles();
        if ((sourceFiles != null) && (sourceFiles.length > 0)) {
            for (File sourceFile : sourceFiles) {
                Path dirName = mkDirIfNotExistsFor(sourceFile);
                try {
                    Files.move(
                            Paths.get(fileSystemConfig.getSourceFolder() + "/" + sourceFile.getName()),
                            Paths.get(dirName + "/" + sourceFile.getName())
                    );
                } catch (IOException e) {
                    log.error("Problem moving file {} to directory {}", sourceFile.getName(), dirName, e);
                }
            }
        }
    }

    private Path mkDirIfNotExistsFor(File file) {
        Path dirName = Paths.get(
                fileSystemConfig.getTargetFolder() + "/"
                        + MONTH_PREFIX_DATE_FORMAT.format(new Date(file.lastModified())) + REMAINING_POSTFIX
        );

        if (!Files.exists(dirName)) {
            try {
                Files.createDirectory(dirName);
            } catch (IOException e) {
                log.error("Problem creating directory {}", dirName, e);
            }
        }

        return dirName;
    }

    private void moveFilesAccordingly(List<MediaItem> items, String albumName) {
        if (items.size() > 0) {
            String createdDir = makeDirectory(fileSystemConfig.getTargetFolder() + "/" + getMinItemsDate(items) + albumName);
            if (createdDir != null) {
                for (MediaItem item : items) {
                    try {
                        Files.move(
                                Paths.get(fileSystemConfig.getSourceFolder() + "/" + item.getFilename()),
                                Paths.get(createdDir + "/" + item.getFilename())
                        );
                    } catch (IOException e) {
                        log.error("Problem moving file {} to {}", item.getFilename(), createdDir);
                    }
                }
            }
        }
    }

    private String makeDirectory(String path) {
        log.info("Creating directory {}", path);
        File directory = new File(path);
        if (directory.mkdir()) {
            return path;
        } else {
            log.error("Problem creating directory {}", path);
            return null;
        }
    }

    /**
     *
     * @param items List of media items from Google Photos.
     * @return The smallest creation date from the items collection formatted according to PREFIX_DATE_FORMAT.
     */
    private String getMinItemsDate(List<MediaItem> items) {
        Optional<Long> minTimestamp = items.stream().map(item -> Timestamps.toMillis(item.getMediaMetadata().getCreationTime())).min(Long::compareTo);

        if (minTimestamp.isPresent()) {
            return PREFIX_DATE_FORMAT.format(new Date(minTimestamp.get()));
        } else {
            log.error("Trying to retrieve minItemsDate, which is not present in collection {}.", items);
            return INVALID_PREFIX;
        }
    }

    private Date getOldestFileDate() throws IOException {
        File sourceDir = new File(fileSystemConfig.getSourceFolder());
        if (sourceDir.exists()) {
            File[] sourceFiles = sourceDir.listFiles();
            if ((sourceFiles != null) && (sourceFiles.length > 0)) {
                return new Date(Arrays.stream(sourceFiles)
                        .map(File::lastModified)
                        .min(Long::compareTo)
                        .get());
            } else {
                throw new IOException("Source directory " + sourceDir.getAbsolutePath() + " is empty");
            }
        } else {
            throw new IOException("Source directory " + sourceDir.getAbsolutePath() + " does not exist");
        }
    }
}
