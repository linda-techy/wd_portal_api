package com.wd.api.service;

import com.wd.api.config.FileUploadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // 0755 = rwxr-xr-x. Nginx (or whatever process serves /storage on the VPS)
    // needs read+execute on each path under the shared storage root; without it,
    // documents/site-reports uploaded by the Java process under a restrictive
    // umask come back as 403 to the customer/portal web apps.
    private static final Set<PosixFilePermission> STORAGE_PERMS =
            PosixFilePermissions.fromString("rwxr-xr-x");

    private final Path fileStorageLocation;

    public FileStorageService(FileUploadConfig fileUploadConfig,
            @org.springframework.beans.factory.annotation.Value("${storageBasePath}") String storageBasePath) {
        String uploadDir = storageBasePath.trim();

        logger.info("Initializing File Storage at: {}", uploadDir);

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
            applyStoragePermissions(destinationFile);

            return subDirectory + "/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Store an uploaded image after re-encoding it as a quality-82 JPEG
     * (V84). Achieves a typical 50–70% size reduction over a raw camera
     * upload (~5 MB → ~1.5–2 MB) with virtually no perceptible quality
     * loss. Pure-Java path via {@link javax.imageio.ImageIO} — no native
     * libs, no platform-specific deployment risk.
     *
     * <p>WebP would be an even-smaller format (~30–40% additional saving)
     * but the only Java WebP encoder ships native binaries, which
     * complicates Linux containerised deployment. When the deployment
     * pipeline is hardened for that, swap the writer mime type from
     * {@code image/jpeg} to {@code image/webp} and add the WebP plugin
     * to the classpath; the rest of this method stays the same.
     *
     * <p>If the input isn't a recognisable image (PDF, video, anything
     * ImageIO can't decode), this method gracefully falls back to
     * {@link #storeFile} so non-image uploads still succeed.
     *
     * @param file       multipart upload from the controller
     * @param subDirectory storage sub-path (e.g. {@code site-reports/42})
     * @return relative storage path (sub-dir + filename) — same shape as
     *         {@link #storeFile} so callers swap freely
     */
    public String storeOptimizedImage(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg");
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
        }

        // Read the upload into a BufferedImage. ImageIO returns null when
        // the bytes aren't a recognised image (PDF, video, plain blob);
        // we fall through to the raw store in that case.
        BufferedImage source;
        try {
            source = ImageIO.read(file.getInputStream());
        } catch (IOException ex) {
            logger.warn("Could not read upload as image, falling back to raw store: {}", ex.getMessage());
            return storeFile(file, subDirectory);
        }
        if (source == null) {
            // Non-image binary — keep as-is.
            return storeFile(file, subDirectory);
        }

        // JPEG doesn't support alpha — for PNGs/etc with transparency,
        // composite onto white before encoding so we don't lose pixels
        // to ImageIO's silent-black behaviour.
        BufferedImage rgb;
        if (source.getColorModel().hasAlpha()) {
            rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
        } else {
            rgb = source;
        }

        try {
            String uniqueFileName = UUID.randomUUID().toString() + ".jpg";
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
            Files.createDirectories(targetLocation);
            Path destinationFile = targetLocation.resolve(uniqueFileName);

            ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.82f);

            try (ImageOutputStream out = ImageIO.createImageOutputStream(destinationFile.toFile())) {
                writer.setOutput(out);
                writer.write(null, new IIOImage(rgb, null, null), params);
            } finally {
                writer.dispose();
            }
            applyStoragePermissions(destinationFile);

            long originalBytes = file.getSize();
            long encodedBytes = Files.size(destinationFile);
            logger.info("Optimised image {} → {}: {} KB → {} KB",
                    originalFileName, uniqueFileName,
                    originalBytes / 1024, encodedBytes / 1024);

            return subDirectory + "/" + uniqueFileName;
        } catch (IOException ex) {
            logger.warn("Image optimisation failed for {}, falling back to raw store: {}",
                    originalFileName, ex.getMessage());
            return storeFile(file, subDirectory);
        }
    }

    /**
     * Set 0755 on a freshly-written file or directory under the storage root.
     * No-op on non-POSIX file systems (Windows dev boxes) — silently swallowed
     * so local builds aren't affected. IOException is logged at WARN, not
     * thrown, because a chmod failure shouldn't fail the upload itself.
     */
    public void applyStoragePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, STORAGE_PERMS);
        } catch (UnsupportedOperationException ignored) {
            // Windows / non-POSIX — chmod has no meaning here.
        } catch (IOException ex) {
            logger.warn("Could not chmod 0755 on {}: {}", path, ex.getMessage());
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

    /** Absolute path to the storage root. Used by reconciliation tools that
     *  need to walk the disk for orphan files. */
    public Path getStorageRoot() {
        return this.fileStorageLocation;
    }
}
