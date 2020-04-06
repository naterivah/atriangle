package tech.artcoded.atriangle.feign.clients.project;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.artcoded.atriangle.api.dto.LogEvent;
import tech.artcoded.atriangle.api.dto.ProjectEvent;
import tech.artcoded.atriangle.api.dto.SinkRequest;

import java.util.List;
import java.util.Map;

public interface ProjectRestFeignClient {

  @PostMapping
  ResponseEntity<ProjectEvent> createProject(@RequestParam("name") String name);

  @GetMapping("/list")
  List<ProjectEvent> findAll();

  @GetMapping("/by-name/{name}")
  ResponseEntity<ProjectEvent> findByName(@PathVariable("name") String name);

  @GetMapping("/by-id/{projectId}")
  ResponseEntity<ProjectEvent> findById(@PathVariable("projectId") String projectId);


  @PostMapping(path = "/add-file",
               consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ProjectEvent> addFile(@RequestPart("file") MultipartFile file,
                                       @RequestParam("projectId") String projectId);

  @PostMapping(path = "/add-sparql-query-template",
               consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ProjectEvent> addFreemarkerSparqlTemplate(@RequestPart("file") MultipartFile file,
                                                           @RequestParam("projectId") String projectId);

  @PostMapping(path = "/execute-select-sparql-query",
               consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<List<Map<String, String>>> executeSelectSparqlQuery(@PathVariable("projectId") String projectId,
                                                                     @PathVariable(
                                                                       "freemarkerTemplateFileId") String freemarkerTemplateFileId,
                                                                     @RequestBody Map<String, String> variables);

  @PostMapping(path = "/execute-construct-sparql-query",
               consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<String> executeConstructSparqlQuery(@PathVariable("projectId") String projectId,
                                                     @PathVariable(
                                                       "freemarkerTemplateFileId") String freemarkerTemplateFileId,
                                                     @RequestBody Map<String, String> variables);

  @PostMapping(path = "/execute-ask-sparql-query",
               consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Boolean> executeAskSparqlQuery(@PathVariable("projectId") String projectId,
                                                @PathVariable(
                                                  "freemarkerTemplateFileId") String freemarkerTemplateFileId,
                                                @RequestBody Map<String, String> variables);

  @GetMapping("/{projectId}/logs")
  ResponseEntity<List<LogEvent>> getLogsForProject(@PathVariable("projectId") String projectId);


  @GetMapping("/{projectId}/download-file/{fileId}")
  ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("projectId") String projectId,
                                                 @PathVariable("fileId") String fileId);

  @DeleteMapping("/{projectId}/delete-file/{fileId}")
  void deleteFile(@PathVariable("projectId") String projectId,
                  @PathVariable("fileId") String fileId);

  @DeleteMapping("/by-name/{name}")
  void deleteByName(@PathVariable("name") String name);

  @DeleteMapping("/by-id/{projectId}")
  void deleteById(@PathVariable("projectId") String projectId);

  @GetMapping("/{projectId}/shacl-validation")
  ResponseEntity<String> shaclValidation(@PathVariable("projectId") String projectId,
                                         @RequestParam("shapesFileId") String shapesFileId,
                                         @RequestParam("rdfModelFileId") String rdfModelFileId);

  @PostMapping("/conversion/skos")
  ResponseEntity<ProjectEvent> skosConversion(
    @RequestParam("projectId") String projectId,
    @RequestParam(value = "labelSkosXl",
                  required = false) boolean labelSkosXl,
    @RequestParam(value = "ignorePostTreatmentsSkos",
                  required = false) boolean ignorePostTreatmentsSkos,
    @RequestParam("xlsFileEventId") String xlsFileEventId
  );

  @PostMapping("/{projectId}/sink")
  ResponseEntity<Void> sink(@RequestBody SinkRequest sinkRequest);
}
