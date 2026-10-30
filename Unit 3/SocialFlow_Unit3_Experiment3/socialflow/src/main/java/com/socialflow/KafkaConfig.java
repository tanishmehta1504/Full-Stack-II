package com.socialflow;

import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConfig {

  @Bean
  NewTopic posts() {
    return TopicBuilder.name("socialflow.posts")
      .partitions(3)
      .replicas(1)
      .build();
  }

  @Bean
  NewTopic dlq() {
    return TopicBuilder.name("socialflow.posts.dlq")
      .partitions(3)
      .replicas(1)
      .build();
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<
    String,
    String
  > kafkaListenerContainerFactory(
    ConsumerFactory<String, String> cf,
    KafkaTemplate<String, String> template,
    @Value("${app.retry-ms}") long delay
  ) {
    var r = new DeadLetterPublishingRecoverer(template, (record, e) ->
      new TopicPartition("socialflow.posts.dlq", record.partition())
    );
    r.setFailIfSendResultIsError(true);
    r.setWaitForSendResultTimeout(Duration.ofSeconds(10));
    var b = new ExponentialBackOffWithMaxRetries(3);
    b.setInitialInterval(delay);
    b.setMultiplier(2);
    b.setMaxInterval(delay * 4);
    var h = new DefaultErrorHandler(r, b);
    h.addNotRetryableExceptions(IllegalArgumentException.class);
    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    f.setCommonErrorHandler(h);
    f.setConcurrency(3);
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return f;
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<String, String> dlqFactory(
    ConsumerFactory<String, String> cf
  ) {
    var f = new ConcurrentKafkaListenerContainerFactory<String, String>();
    f.setConsumerFactory(cf);
    f.setCommonErrorHandler(
      new DefaultErrorHandler(
        new org.springframework.util.backoff.FixedBackOff(1000, Long.MAX_VALUE)
      )
    );
    f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return f;
  }
}
