package tech.artcoded.atriangle.rest.project;

import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import tech.artcoded.atriangle.api.dto.CommonConstants;
import tech.artcoded.atriangle.api.dto.FileEvent;
import tech.artcoded.atriangle.api.dto.ProjectEvent;
import tech.artcoded.atriangle.api.dto.SinkRequest;
import tech.artcoded.atriangle.core.rest.annotation.CrossOriginRestController;
import tech.artcoded.atriangle.core.rest.controller.BuildInfoControllerTrait;
import tech.artcoded.atriangle.core.rest.controller.PingControllerTrait;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@CrossOriginRestController
@ApiOperation("Project Rest")
@Slf4j
public class ProjectRestController implements PingControllerTrait, BuildInfoControllerTrait {
  private final ProjectRestService projectRestService;
  private final ProjectSinkProducer projectSinkProducer;

  @Getter
  private final BuildProperties buildProperties;

  @Inject
  public ProjectRestController(ProjectRestService projectRestService,
                               ProjectSinkProducer projectSinkProducer,
                               BuildProperties buildProperties) {
    this.projectRestService = projectRestService;
    this.projectSinkProducer = projectSinkProducer;
    this.buildProperties = buildProperties;
  }

  @PostMapping
  public ResponseEntity<ProjectEvent> createProject(@RequestParam("name") String name) {
    return ResponseEntity.ok(projectRestService.newProject(name));
  }

  @PutMapping(path = "/add-file",
              consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProjectEvent> addFile(@RequestParam("file") MultipartFile multipartFile,
                                              @RequestParam("projectId") String projectId) {
    return projectRestService.addFile(projectId, multipartFile).map(ResponseEntity::ok)
                             .orElseGet(ResponseEntity.badRequest()::build);
  }

  @GetMapping("/by-name/{name}")
  public ResponseEntity<ProjectEvent> findByName(@PathVariable("name") String name) {
    return projectRestService.findByName(name).map(ResponseEntity::ok)
                             .orElseGet(ResponseEntity.notFound()::build);
  }

  @GetMapping("/{projectId}/download-file/{fileId}")
  public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable("projectId") String projectId,
                                                        @PathVariable("fileId") String fileId) {
    return projectRestService.downloadFile(projectId, fileId);
  }

  @GetMapping("/{projectId}/shacl-validation")
  public ResponseEntity<String> shaclValidation(@PathVariable("projectId") String projectId,
                                                @RequestParam("shapesFileId") String shapesFileId,
                                                @RequestParam("rdfModelFileId") String rdfModelFileId) {
    return projectRestService.shaclValidation(projectId, shapesFileId, rdfModelFileId);
  }

  @DeleteMapping("/{projectId}/delete-file/{fileId}")
  public void deleteFile(@PathVariable("projectId") String projectId,
                         @PathVariable("fileId") String fileId) {
    CompletableFuture.runAsync(() -> projectRestService.deleteFile(projectId, fileId));
  }

  @DeleteMapping("/by-name/{name}")
  public void deleteByName(@PathVariable("name") String name) {
    projectRestService.deleteByName(name);
  }

  @DeleteMapping("/by-id/{projectId}")
  public void deleteById(@PathVariable("projectId") String id) {
    projectRestService.deleteById(id);
  }

  @GetMapping("/by-id/{projectId}")
  public ResponseEntity<ProjectEvent> findById(@PathVariable("projectId") String id) {
    return projectRestService.findById(id).map(ResponseEntity::ok)
                             .orElseGet(ResponseEntity.notFound()::build);
  }

  @GetMapping("/list")
  public List<ProjectEvent> findAll() {
    return projectRestService.findAll();
  }

  @GetMapping("/ping-skos")
  public ResponseEntity<Map<String, String>> pingSkos() {
    return projectRestService.skosPing();
  }

  @PostMapping("/skos-conversion")
  public ResponseEntity<ProjectEvent> skosConversion(
    @RequestParam("projectId") String projectId,
    @RequestParam(value = "labelSkosXl",
                  required = false,
                  defaultValue = "false") boolean labelSkosXl,
    @RequestParam(value = "ignorePostTreatmentsSkos",
                  required = false,
                  defaultValue = "false") boolean ignorePostTreatmentsSkos,
    @RequestParam("xlsFileEventId") String xlsFileEventId
  ) {
    FileEvent xlsFileEvent = projectRestService.getFileMetadata(projectId, xlsFileEventId)
                                               .orElseThrow(() -> new RuntimeException("file  not found"));
    if (!CommonConstants.XLSX_MEDIA_TYPE.equals(xlsFileEvent.getContentType())) {
      log.error("only xlsx type supported, provided {}", xlsFileEvent.getContentType());
      return ResponseEntity.badRequest().build();
    }
    return projectRestService.skosConversion(projectId, labelSkosXl, ignorePostTreatmentsSkos, xlsFileEvent)
                             .map(ResponseEntity::ok)
                             .orElseGet(ResponseEntity.badRequest()::build);
  }

  @PostMapping("/{projectId}/sink")
  public ResponseEntity<Void> sink(@RequestBody SinkRequest sinkRequest) {
    projectSinkProducer.sink(sinkRequest);
    return ResponseEntity.accepted().build();
  }


}