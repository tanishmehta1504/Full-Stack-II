package com.postflow;

import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcher {
 private final JdbcTemplate db;private final KafkaTemplate<String,String> kafka;private final EventService events;private final String topic;
 public OutboxDispatcher(JdbcTemplate db,KafkaTemplate<String,String> kafka,EventService events,@Value("${app.topic}") String topic){this.db=db;this.kafka=kafka;this.events=events;this.topic=topic;}
 @Scheduled(fixedDelayString="${app.dispatch-ms}")
 public void dispatch(){
  long now=EventService.now();
  var rows=db.queryForList("SELECT * FROM outbox WHERE sent_at IS NULL AND due_at<=? AND next_try<=? ORDER BY due_at LIMIT 20",now,now);
  for(var row:rows){String event=(String)row.get("EVENT_ID"),id=(String)row.get("ID");
   try{
    // Kafka ACK first, then mark sent. A crash between the two may resend; the consumer is idempotent.
    var result=kafka.send(topic,event,(String)row.get("PAYLOAD")).get(15,TimeUnit.SECONDS);
    db.update("UPDATE outbox SET sent_at=?,last_error=NULL WHERE id=?",EventService.now(),id);
    db.update("UPDATE events SET status='QUEUED',updated_at=? WHERE id=? AND status='SCHEDULED'",EventService.now(),event);
    events.log(event,"KAFKA_ACK","Broker acknowledged partition "+result.getRecordMetadata().partition()+", offset "+result.getRecordMetadata().offset());
   }catch(Exception ex){
    if(ex instanceof InterruptedException){Thread.currentThread().interrupt();return;}
    int attempt=((Number)row.get("ATTEMPTS")).intValue()+1;
    long delay=Math.min(30000,1000L*(1L<<Math.min(attempt-1,5)));
    db.update("UPDATE outbox SET attempts=?,next_try=?,last_error=? WHERE id=?",attempt,EventService.now()+delay,"Kafka delivery not acknowledged; retry scheduled",id);
    events.log(event,"OUTBOX_RETRY","Kafka send unconfirmed; durable outbox will retry in "+delay+" ms");
   }
  }
 }
}
