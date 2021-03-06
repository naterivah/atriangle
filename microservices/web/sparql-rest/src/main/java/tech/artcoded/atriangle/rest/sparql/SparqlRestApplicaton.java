package tech.artcoded.atriangle.rest.sparql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import tech.artcoded.atriangle.core.kafka.KafkaConfig;
import tech.artcoded.atriangle.core.rest.config.SwaggerConfig;
import tech.artcoded.atriangle.core.sparql.SparqlConfig;
import tech.artcoded.atriangle.feign.clients.file.DiscoverableFileRestFeignClient;
import tech.artcoded.atriangle.feign.clients.util.FeignExceptionHandler;

@SpringBootApplication
@Import({KafkaConfig.class, SwaggerConfig.class, FeignExceptionHandler.class, SparqlConfig.class})
@EnableFeignClients(clients = {DiscoverableFileRestFeignClient.class})
@EnableDiscoveryClient
public class SparqlRestApplicaton {
  public static void main(String[] args) {
    SpringApplication.run(SparqlRestApplicaton.class, args);
  }
}
