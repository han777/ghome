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
        if (uploadPath == null || uploadPath.isEmpty()) {
            return System.getProperty("user.dir") + "/uploads/";
        }
        // If relative path, prepend project root
        if (!uploadPath.startsWith("file://") && !java.nio.file.Paths.get(uploadPath).isAbsolute()) {
            return System.getProperty("user.dir") + uploadPath.replaceFirst("^\\./", "");
        }
        return uploadPath;
    }
}
