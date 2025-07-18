package com.intellimart.productservice.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource; // !!! NEW IMPORT
import org.springframework.core.io.UrlResource; // !!! NEW IMPORT
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException; // !!! NEW IMPORT
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// Custom exception for storage related issues (optional but good practice)
class ImageStorageException extends RuntimeException {
    public ImageStorageException(String message) {
        super(message);
    }

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Custom exception for file not found (optional but good practice)
class ImageFileNotFoundException extends RuntimeException {
    public ImageFileNotFoundException(String message) {
        super(message);
    }

    public ImageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}


@Service
public class ImageStorageService {

    @Value("${product.images.upload-dir}")
    private String uploadDir;
    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            System.out.println("Product image upload directory created: " + this.fileStorageLocation.toString());
        } catch (IOException ex) {
            // Use a custom exception for better context
            throw new ImageStorageException("Could not create the upload directory: " + this.fileStorageLocation.toString(), ex);
        }
    }

    /**
     * Stores a MultipartFile and returns its relative URL/path.
     * Generates a unique file name to prevent collisions.
     *
     * @param file The MultipartFile to store.
     * @return The relative path/URL of the stored image (e.g., "/images/unique-id_filename.jpg").
     * @throws ImageStorageException if file storage fails.
     */
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ImageStorageException("Failed to store empty file.");
        }

        String originalFileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());
        // Extract file extension
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(dotIndex);
        }

        // Generate a unique file name using UUID
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Check if the file's name contains invalid characters (e.g., path traversals)
            if (uniqueFileName.contains("..")) {
                throw new ImageStorageException("Filename contains invalid path sequence " + uniqueFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the relative URL/path that will be stored in the database
            // This URL corresponds to the path served by WebConfig
            return "/images/" + uniqueFileName;
        } catch (IOException ex) {
            throw new ImageStorageException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Loads a file as a Spring Resource, typically for serving it via a controller.
     * This method assumes the input 'fileName' is the unique generated file name
     * (e.g., "unique-id_originalname.jpg"), not the full "/images/..." path.
     *
     * @param fileName The unique name of the file to load (e.g., from database).
     * @return Resource representing the file.
     * @throws ImageFileNotFoundException if the file does not exist.
     * @throws ImageStorageException if there's an issue with URL conversion.
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            // Remove the /images/ prefix if it's present in the fileName from the database
            String cleanedFileName = fileName.startsWith("/images/") ? fileName.substring("/images/".length()) : fileName;
            Path filePath = this.fileStorageLocation.resolve(cleanedFileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ImageFileNotFoundException("File not found or not readable " + cleanedFileName);
            }
        } catch (MalformedURLException ex) {
            throw new ImageStorageException("Error converting file path to URL for " + fileName, ex);
        }
    }

    /**
     * Deletes a file from the storage location.
     * This method assumes the input 'fileName' is the unique generated file name
     * (e.g., "unique-id_originalname.jpg"), not the full "/images/..." path.
     *
     * @param fileName The unique name of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     * @throws ImageStorageException if there's an I/O error during deletion.
     */
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false; // Nothing to delete
        }
        // Remove the /images/ prefix if it's present in the fileName from the database
        String cleanedFileName = fileName.startsWith("/images/") ? fileName.substring("/images/".length()) : fileName;
        Path filePath = this.fileStorageLocation.resolve(cleanedFileName).normalize();
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("Deleted file: " + filePath.toString());
                return true;
            } else {
                System.out.println("File not found, skipping deletion: " + filePath.toString());
                return false;
            }
        } catch (IOException ex) {
            throw new ImageStorageException("Could not delete file: " + cleanedFileName, ex);
        }
    }
}