package com.wd.api.service;

import com.wd.api.config.FileUploadConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(FileUploadConfig fileUploadConfig,
            @org.springframework.beans.factory.annotation.Value("${storageBasePath:N:\\Projects\\wd projects git\\storage}") String storageBasePath) {
        String uploadDir = storageBasePath != null && !storageBasePath.trim().isEmpty() 
            ? storageBasePath.trim() 
            : "N:\\Projects\\wd projects git\\storage";

        System.out.println("Initializing File Storage at: " + uploadDir);

        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            // Create standard subdirectories
            Files.createDirectories(this.fileStorageLocation.resolve("documents"));
            Files.createDirectories(this.fileStorageLocation.resolve("quotations/pdfs"));
            Files.createDirectories(this.fileStorageLocation.resolve("leads/documents"));
            Files.createDirectories(this.fileStorageLocation.resolve("projects/documents"));
            Files.createDirectories(this.fileStorageLocation.resolve("uploads"));
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            // Generate unique filename
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Create subdirectory if it doesn't exist
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);

            // Copy file to the target location
            Path destinationFile = targetLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            return subDirectory + "/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path file = this.fileStorageLocation.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + filePath, ex);
        }
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }
}
