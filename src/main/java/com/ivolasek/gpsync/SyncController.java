package com.ivolasek.gpsync;

import com.ivolasek.gpsync.filesystem.FileWalkerService;
import com.ivolasek.gpsync.google.GoogleAuthException;
import com.ivolasek.gpsync.google.PhotosService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;

@RestController
@Slf4j
public class SyncController {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Autowired
    private PhotosService photosService;

    @Autowired
    private FileWalkerService fileWalkerService;

    @RequestMapping("/")
    public String index(HttpServletResponse response, @RequestParam(name = "code", required = false) String code) throws IOException, GoogleAuthException {
        if (code != null) {
            photosService.setAuthGrantCode(code);
        }

        try {
            log.info("Retrieving albums...");
            fileWalkerService.walkFiles();
        } catch (IllegalStateException e) {
            try {
                response.sendRedirect(photosService.getAuthorizationUrl());
            } catch (IOException ex) {
                log.error("Problem creating Google Auth Flow", ex);
                response.sendError(500, "Problem creating Google Auth Flow");
                return "Problem syncing photos";
            }
        } catch (IOException e) {
            log.error("Problem accessing local files", e);
        }

        return "Photos synced";
    }
}
