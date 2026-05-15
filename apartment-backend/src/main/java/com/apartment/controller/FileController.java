package com.apartment.controller;

import com.apartment.service.FilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FilesService filesService;

    @PostMapping("/upload")
    public java.util.Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String uploadDir = filesService.getUploadDir();
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID().toString() + extension;
        File dest = new File(uploadDir + fileName);
        file.transferTo(dest);

        return java.util.Collections.singletonMap("url", "/uploads/" + fileName);
    }
}
