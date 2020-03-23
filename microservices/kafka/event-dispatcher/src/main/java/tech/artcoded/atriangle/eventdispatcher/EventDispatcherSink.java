package tech.artcoded.atriangle.eventdispatcher;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tech.artcoded.atriangle.api.CheckedFunction;
import tech.artcoded.atriangle.api.ObjectMapperWrapper;
import tech.artcoded.atriangle.api.dto.KafkaEvent;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Function;

@Component
@Slf4j
public class EventDispatcherSink {
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapperWrapper mapperWrapper;

  @Value("${event.dispatcher.elastic-sink-topic}")
  private String elsticSinkTopic;
  @Value("${event.dispatcher.rdf-sink-topic}")
  private String rdfSinkTopic;
  @Value("${event.dispatcher.rdf-sink-topic-out}")
  private String rdfSinkTopicOut;
  @Value("${event.dispatcher.elastic-sink-topic-out}")
  private String elasticSinkTopicOut;
  @Value("${event.dispatcher.mongodb-sink-topic}")
  private String mongoSinkTopic;
  @Value("${event.dispatcher.mongodb-sink-topic-out}")
  private String mongoSinkTopicOut;


  @Inject
  public EventDispatcherSink(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapperWrapper mapperWrapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.mapperWrapper = mapperWrapper;
  }

  @KafkaListener(topics = "${spring.kafka.template.default-topic}")
  public void dispatch(ConsumerRecord<String, String> event) throws Exception {
    Function<String, SendResult<String, String>> sendEvent = CheckedFunction.toFunction((topic) ->
                                                                                          kafkaTemplate.send(new ProducerRecord<>(topic, event
                                                                                            .key(), event.value()))
                                                                                                       .get());
    String value = event.value();
    Optional<KafkaEvent> optionalKafkaEvent = mapperWrapper.deserialize(value, KafkaEvent.class);

    optionalKafkaEvent.ifPresent(kafkaEvent -> {
      switch (kafkaEvent.getEventType()) {
        case RDF_SINK:
          log.info("result of send event: {}", sendEvent.apply(rdfSinkTopic));
          break;
        case MONGODB_SINK:
          log.info("result of send event: {}", sendEvent.apply(mongoSinkTopic));
          break;
        case ELASTIC_SINK:
          log.info("result of send event: {}", sendEvent.apply(elsticSinkTopic));
          break;
        case RDF_SINK_OUT:
          log.info("result of send event: {}", sendEvent.apply(rdfSinkTopicOut));
          break;
        case ELASTIC_SINK_OUT:
          log.info("result of send event: {}", sendEvent.apply(elasticSinkTopicOut));
          break;
        case MONGODB_SINK_OUT:
          log.info("result of send event: {}", sendEvent.apply(mongoSinkTopicOut));
          break;
        default:
          throw new RuntimeException("not supported yet");
      }
    });
  }

}