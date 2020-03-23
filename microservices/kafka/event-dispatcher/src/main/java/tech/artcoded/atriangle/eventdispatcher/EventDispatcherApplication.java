package tech.artcoded.atriangle.eventdispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import tech.artcoded.atriangle.core.kafka.KafkaConfig;

@SpringBootApplication
@Import({KafkaConfig.class})
public class EventDispatcherApplication {
  public static void main(String[] args) {
    SpringApplication.run(EventDispatcherApplication.class, args);
  }

}