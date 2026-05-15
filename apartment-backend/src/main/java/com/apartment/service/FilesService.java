package com.apartment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for file upload path configuration.
 */
@Service
public class FilesService {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    /**
     * Get the absolute path to the upload directory.
     */
    public String getUploadDir() {
        String path = uploadPath;
        if (path == null || path.isEmpty()) {
            path = System.getProperty("user.dir") + "/uploads";
        } else if (!path.startsWith("file://") && !java.nio.file.Paths.get(path).isAbsolute()) {
            path = System.getProperty("user.dir") + "/" + path.replaceFirst("^\\./", "");
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        return path;
    }
}
