package com.infy.pinterest.utility;
import com.infy.pinterest.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {
    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png",
            "gif", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("Please select a file to upload");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException("File size must not exceed 10MB");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileUploadException("Only image files (jpg, jpeg, png, gif, webp) are allowed");
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String filename = UUID.randomUUID().toString() + "." + extension;
            Path filePath = uploadPath.resolve(filename);

            // Copy file to destination
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {}", filename);

            // Return URL (in production, return full URL with domain)
            return "/uploads/" + filename;

        } catch (IOException ex) {
            throw new FileUploadException("Failed to upload file: " + ex.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }


}
