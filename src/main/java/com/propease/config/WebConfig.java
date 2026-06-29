package com.propease.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Value("${app.images.directory:uploads}")
  private String directory;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**")
        .addResourceLocations(Paths.get(directory).toAbsolutePath().normalize().toUri().toString());
  }
}
