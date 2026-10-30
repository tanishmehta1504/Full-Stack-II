package com.postflow;

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
 @Bean NewTopic posts(@Value("${app.topic}") String topic){return TopicBuilder.name(topic).partitions(3).replicas(1).build();}
 @Bean NewTopic deadLetters(@Value("${app.dlq-topic}") String topic){return TopicBuilder.name(topic).partitions(3).replicas(1).build();}
 @Bean DefaultErrorHandler errorHandler(KafkaTemplate<String,String> template,EventService events,
   @Value("${app.dlq-topic}") String dlq,@Value("${app.retry-initial-ms}") long initial){
  var recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->new TopicPartition(dlq,record.partition()));
  recoverer.setFailIfSendResultIsError(true);
  recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(15));
  var backoff=new ExponentialBackOffWithMaxRetries(3);
  backoff.setInitialInterval(initial);backoff.setMultiplier(2);backoff.setMaxInterval(initial*4);
  var handler=new DefaultErrorHandler(recoverer,backoff);
  handler.addNotRetryableExceptions(IllegalArgumentException.class,org.springframework.web.server.ResponseStatusException.class);
  handler.setRetryListeners((record,ex,attempt)->{
   for(Throwable cause=ex;cause!=null;cause=cause.getCause())if(cause instanceof IllegalArgumentException || cause instanceof org.springframework.web.server.ResponseStatusException)return;
   if(attempt<=3) events.log(record.key()==null?null:EventService.clip(String.valueOf(record.key()),36),"RETRY_WAIT","Delivery failure "+attempt+"; backoff "+(initial*(1L<<(attempt-1)))+" ms for retryable errors");
  });
  return handler;
 }
 @Bean ConcurrentKafkaListenerContainerFactory<String,String> kafkaListenerContainerFactory(ConsumerFactory<String,String> consumers,DefaultErrorHandler handler){
  var factory=new ConcurrentKafkaListenerContainerFactory<String,String>();factory.setConsumerFactory(consumers);
  factory.setConcurrency(3);factory.setCommonErrorHandler(handler);
  factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
  return factory;
 }
 /** Never recover a failed DLQ database write back into the same DLQ topic. Keep retrying it. */
 @Bean ConcurrentKafkaListenerContainerFactory<String,String> dlqFactory(ConsumerFactory<String,String> consumers){
  var factory=new ConcurrentKafkaListenerContainerFactory<String,String>();factory.setConsumerFactory(consumers);
  factory.setCommonErrorHandler(new DefaultErrorHandler(new org.springframework.util.backoff.FixedBackOff(2000,Long.MAX_VALUE)));
  factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);return factory;
 }
}
