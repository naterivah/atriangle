package tech.artcoded.atriangle.rest.elastic;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tech.artcoded.atriangle.api.IdGenerators;
import tech.artcoded.atriangle.api.ObjectMapperWrapper;
import tech.artcoded.atriangle.api.dto.LogEvent;
import tech.artcoded.atriangle.core.elastic.ElasticSearchRdfService;
import tech.artcoded.atriangle.core.rest.annotation.SwaggerHeaderAuthentication;
import tech.artcoded.atriangle.core.rest.controller.BuildInfoControllerTrait;
import tech.artcoded.atriangle.core.rest.controller.PingControllerTrait;
import tech.artcoded.atriangle.feign.clients.elastic.ElasticRestFeignClient;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Slf4j
public class ElasticRestController
    implements PingControllerTrait, BuildInfoControllerTrait, ElasticRestFeignClient {
  @Getter private final BuildProperties buildProperties;
  private final ElasticSearchRdfService elasticSearchRdfService;
  private final ObjectMapperWrapper mapperWrapper;

  @Value("${elasticsearch.shared-indexes.logsink}")
  private String logSinkIndex;

  @Inject
  public ElasticRestController(
      BuildProperties buildProperties,
      ElasticSearchRdfService elasticSearchRdfService,
      ObjectMapperWrapper mapperWrapper) {
    this.buildProperties = buildProperties;
    this.elasticSearchRdfService = elasticSearchRdfService;
    this.mapperWrapper = mapperWrapper;
  }

  @Override
  @SwaggerHeaderAuthentication
  public List<LogEvent> getLogsByCorrelationId(String correlationId) {
    SearchResponse searchResponse =
        elasticSearchRdfService.matchQuery("correlationId", correlationId, logSinkIndex);
    return Stream.of(searchResponse.getHits().getHits())
        .map(hit -> mapperWrapper.deserialize(hit.getSourceAsString(), LogEvent.class))
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(LogEvent::getCreationDate, Date::compareTo))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> createIndex(
      String indexName, boolean deleteIndexIfExist, String elasticConfiguration) {

    boolean indexExist = elasticSearchRdfService.indexExist(indexName);

    if (indexExist) {
      if (!deleteIndexIfExist) {
        return ResponseEntity.badRequest().body("index already exists");
      }
      log.info("deleting index {}", indexName);
      AcknowledgedResponse acknowledgedResponse = elasticSearchRdfService.deleteIndex(indexName);
      log.info("delete index response: {}", acknowledgedResponse.isAcknowledged());
    }

    CreateIndexResponse response =
        elasticSearchRdfService.createIndex(
            indexName,
            IOUtils.toInputStream(
                Optional.ofNullable(elasticConfiguration).orElse("{}"), StandardCharsets.UTF_8));
    log.info("index creation response: {}", response.isAcknowledged());
    return ResponseEntity.ok("elastic index created");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> deleteIndex(String indexName) {
    AcknowledgedResponse acknowledgedResponse = elasticSearchRdfService.deleteIndex(indexName);
    log.info("delete index ack {}", acknowledgedResponse.isAcknowledged());
    return ResponseEntity.ok("index deleted");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> deleteDocument(String indexName, String uuid) {
    DeleteResponse deleteResponse = elasticSearchRdfService.deleteDocument(indexName, uuid);
    log.info("delete document result {}", deleteResponse.getResult().getLowercase());
    return ResponseEntity.ok("document deleted");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> index(String indexName, String document) {
    elasticSearchRdfService.indexAsync(indexName, IdGenerators.get(), document);
    return ResponseEntity.ok("resource indexed on elastic");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<Set<String>> indices() {
    return ResponseEntity.ok(elasticSearchRdfService.indices());
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> search(String indexName, String request) {
    SearchResponse searchResponse = elasticSearchRdfService.rawSearch(indexName, request);
    String jsonResponse = searchResponse.toString();
    return ResponseEntity.ok(jsonResponse);
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> findAll(String indexName) {
    SearchResponse searchResponse = elasticSearchRdfService.searchAll(indexName);
    String jsonResponse = searchResponse.toString();
    return ResponseEntity.ok(jsonResponse);
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> updateSettings(
      String indexName, boolean preserveSettings, String settings) {
    AcknowledgedResponse acknowledgedResponse =
        elasticSearchRdfService.updateSettings(indexName, settings, preserveSettings);
    log.info("acknowledge for update settings {}", acknowledgedResponse.isAcknowledged());
    return ResponseEntity.ok("settings updated");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> getSettings(String indexName) {
    GetSettingsResponse settings = elasticSearchRdfService.getSettings(indexName);
    return ResponseEntity.ok(settings.toString());
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<String> updateMapping(String indexName, String mapping) {
    AcknowledgedResponse acknowledgedResponse =
        elasticSearchRdfService.updateMappings(indexName, mapping);
    log.info("acknowledge for update mapping {}", acknowledgedResponse.isAcknowledged());
    return ResponseEntity.ok("mapping updated");
  }

  @Override
  @SwaggerHeaderAuthentication
  public ResponseEntity<Map<String, Object>> getMapping(String indexName) {
    GetMappingsResponse mapping = elasticSearchRdfService.getMappings(indexName);
    return ResponseEntity.ok(
        mapping.mappings().entrySet().stream()
            .map(m -> Map.entry(m.getKey(), m.getValue().getSourceAsMap()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }
}
