package com.propease.service;

import com.propease.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalImageStorageService implements ImageStorageService {
  private final Path root;
  private final String baseUrl;

  public LocalImageStorageService(
      @Value("${app.images.directory:uploads}") String directory,
      @Value("${app.images.base-url:http://localhost:8080/uploads}") String baseUrl)
      throws IOException {
    this.root = Paths.get(directory).toAbsolutePath().normalize();
    this.baseUrl = baseUrl;
    Files.createDirectories(root);
  }

  @Override
  public String store(MultipartFile f) {
    if (f.isEmpty() || f.getContentType() == null || !f.getContentType().startsWith("image/"))
      throw new BusinessException("Only non-empty image files are allowed");
    String ext =
        Optional.ofNullable(f.getOriginalFilename())
            .filter(n -> n.contains("."))
            .map(n -> n.substring(n.lastIndexOf('.')))
            .orElse("");
    String name = UUID.randomUUID() + ext;
    try {
      Files.copy(f.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
      return baseUrl + "/" + name;
    } catch (IOException e) {
      throw new BusinessException("Unable to store image");
    }
  }
}
