package com.propease.config;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointLogger {

  private static final String CONTROLLER_PACKAGE = "com.propease.controller";

  private final RequestMappingHandlerMapping handlerMapping;

  @Value("${server.port:8080}")
  private int serverPort;

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
  private String swaggerUiPath;

  @Value("${springdoc.api-docs.path:/v3/api-docs}")
  private String openApiPath;

  @EventListener(ApplicationReadyEvent.class)
  public void logEndpoints() {
    String baseUrl = "http://localhost:" + serverPort + normalizeContextPath(contextPath);
    List<Endpoint> endpoints =
        handlerMapping.getHandlerMethods().entrySet().stream()
            .filter(entry -> isApplicationController(entry.getValue()))
            .flatMap(
                entry ->
                    entry.getKey().getPatternValues().stream()
                        .map(path -> toEndpoint(entry.getKey(), entry.getValue(), path)))
            .sorted(Comparator.comparing(Endpoint::path).thenComparing(Endpoint::methods))
            .toList();

    StringBuilder catalog = new StringBuilder();
    catalog.append(System.lineSeparator());
    catalog
        .append("================ PROP EASE API ================")
        .append(System.lineSeparator());
    catalog
        .append("Swagger UI : ")
        .append(baseUrl)
        .append(swaggerUiPath)
        .append(System.lineSeparator());
    catalog
        .append("OpenAPI JSON: ")
        .append(baseUrl)
        .append(openApiPath)
        .append(System.lineSeparator());
    catalog.append("API endpoints:").append(System.lineSeparator());
    endpoints.forEach(
        endpoint ->
            catalog
                .append(String.format("  %-13s %s", endpoint.methods(), endpoint.path()))
                .append("  -> ")
                .append(endpoint.handler())
                .append(System.lineSeparator()));
    catalog.append("================================================");

    log.info("{}", catalog);
  }

  private boolean isApplicationController(HandlerMethod handlerMethod) {
    Package controllerPackage = handlerMethod.getBeanType().getPackage();
    return controllerPackage != null && controllerPackage.getName().startsWith(CONTROLLER_PACKAGE);
  }

  private Endpoint toEndpoint(
      RequestMappingInfo mapping, HandlerMethod handlerMethod, String path) {
    Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
    String methodNames =
        methods.isEmpty()
            ? "ALL"
            : methods.stream()
                .map(RequestMethod::name)
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("ALL");
    String handler =
        handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
    return new Endpoint(methodNames, path, handler);
  }

  private String normalizeContextPath(String value) {
    if (value == null || value.isBlank() || "/".equals(value)) {
      return "";
    }
    return value.startsWith("/") ? value : "/" + value;
  }

  private record Endpoint(String methods, String path, String handler) {}
}
