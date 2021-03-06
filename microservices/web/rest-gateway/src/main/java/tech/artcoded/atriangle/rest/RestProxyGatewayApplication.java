package tech.artcoded.atriangle.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Import;
import tech.artcoded.atriangle.core.database.JpaAuditingConfiguration;

@SpringBootApplication
@Import(JpaAuditingConfiguration.class)
@EnableZuulProxy
@EnableDiscoveryClient
public class RestProxyGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(RestProxyGatewayApplication.class, args);
  }
}
