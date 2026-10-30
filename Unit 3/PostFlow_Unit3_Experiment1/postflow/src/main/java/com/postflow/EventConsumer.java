package com.postflow;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {
 private final EventService events;private final ObjectMapper json;
 public EventConsumer(EventService events,ObjectMapper json){this.events=events;this.json=json;}
 @KafkaListener(id="post-workers",groupId="${spring.kafka.consumer.group-id}",topics="${app.topic}")
 public void consume(ConsumerRecord<String,String> record){
  PostEvent event;
  try{event=json.readValue(record.value(),PostEvent.class);}
  catch(Exception ex){throw new IllegalArgumentException("Malformed post event JSON",ex);}
  events.process(event);
 }
 @KafkaListener(id="dead-letter-observer",topics="${app.dlq-topic}",groupId="postflow-dlq-observer-v1",containerFactory="dlqFactory")
 public void deadLetter(ConsumerRecord<String,String> record){
  PostEvent event=null;try{event=json.readValue(record.value(),PostEvent.class);}catch(Exception ignored){}
  String source=text(record,KafkaHeaders.DLT_ORIGINAL_TOPIC,record.topic());
  int partition=(int)number(record,KafkaHeaders.DLT_ORIGINAL_PARTITION,record.partition());
  long offset=number(record,KafkaHeaders.DLT_ORIGINAL_OFFSET,record.offset());
  String key=UUID.nameUUIDFromBytes((source+":"+partition+":"+offset).getBytes(StandardCharsets.UTF_8)).toString();
  String reason=text(record,KafkaHeaders.DLT_EXCEPTION_MESSAGE,"Retries exhausted");
  events.deadLetter(key,event,record.value(),reason,source,partition,offset);
 }
 private String text(ConsumerRecord<?,?> r,String name,String fallback){var h=r.headers().lastHeader(name);return h==null?fallback:new String(h.value(),StandardCharsets.UTF_8);}
 private long number(ConsumerRecord<?,?> r,String name,long fallback){var h=r.headers().lastHeader(name);if(h==null)return fallback;var b=ByteBuffer.wrap(h.value());return h.value().length==8?b.getLong():b.getInt();}
}
