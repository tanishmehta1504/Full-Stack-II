package com.postflow;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {
 private final JdbcTemplate db; private final ObjectMapper json;
 public EventService(JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public static long now(){return System.currentTimeMillis();}
 public static String id(){return UUID.randomUUID().toString();}
 public void log(String event,String kind,String message){db.update("INSERT INTO activity VALUES(?,?,?,?,?)",id(),event,kind,message,now());}
 @Transactional public String create(String title,String content,String channel,String mode,long scheduledAt){
  String id=id();long now=now();
  db.update("INSERT INTO events(id,title,content,channel,failure_mode,status,scheduled_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",id,title,content,channel,mode,"SCHEDULED",scheduledAt,now,now);
  enqueue(new PostEvent(id,0,title,content,channel,scheduledAt),scheduledAt);
  log(id,"SCHEDULED","Saved post and outbox event in one database transaction");return id;
 }
 private void enqueue(PostEvent event,long due){
  try {db.update("INSERT INTO outbox(id,event_id,payload,due_at) VALUES(?,?,?,?)",id(),event.eventId(),json.writeValueAsString(event),due);}
  catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
 }
 public Map<String,Object> event(String id){
  var rows=db.queryForList("SELECT * FROM events WHERE id=?",id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found");return rows.get(0);
 }
 @Transactional public void delete(String id){
  var e=locked(id);
  String status=(String)e.get("STATUS");
  if("PUBLISHED".equals(status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Published events cannot be deleted; they are part of the audit trail");
  if("RETRYING".equals(status)||"QUEUED".equals(status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Event is currently being processed. Wait for it to complete before deleting");
  db.update("DELETE FROM dead_letters WHERE event_id=?",id);
  db.update("DELETE FROM processed_events WHERE event_id=?",id);
  db.update("DELETE FROM publications WHERE event_id=?",id);
  db.update("DELETE FROM activity WHERE event_id=?",id);
  db.update("DELETE FROM outbox WHERE event_id=?",id);
  db.update("DELETE FROM events WHERE id=?",id);
 }
 @Transactional public void duplicate(String id){
  // Serialize duplicate/replay operations against the same business event.
  var e=locked(id);
  if(!"PUBLISHED".equals(e.get("STATUS")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Publish the event first, then send a duplicate");
  enqueue(fromRow(e),now());log(id,"DUPLICATE_SENT","Same event ID queued again; publication must remain unchanged");
 }
 @Transactional public void replay(String dlqId){
  var rows=db.queryForList("SELECT * FROM dead_letters WHERE id=? FOR UPDATE",dlqId);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Dead letter not found");
  var dead=rows.get(0);
  if(Boolean.TRUE.equals(dead.get("REPLAYED")))throw new ResponseStatusException(HttpStatus.CONFLICT,"This dead letter was already replayed");
  String eventId=(String)dead.get("EVENT_ID");
  if(eventId==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Malformed events cannot be replayed; inspect the payload");
  var e=locked(eventId);
  if(!"DLQ".equals(e.get("STATUS")) || !Objects.equals(e.get("GENERATION"),dead.get("GENERATION")))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Event is already resolved or a newer replay is in progress");
  int generation=((Number)e.get("GENERATION")).intValue()+1;
  // Demo remediation: remove the simulated failure. The business event ID remains unchanged.
  db.update("UPDATE events SET generation=?,failure_mode='NONE',status='SCHEDULED',attempts=0,last_error=NULL,updated_at=? WHERE id=?",generation,now(),eventId);
  enqueue(new PostEvent(eventId,generation,(String)e.get("TITLE"),(String)e.get("CONTENT"),(String)e.get("CHANNEL"),now()),now());
  db.update("UPDATE dead_letters SET replayed=TRUE WHERE id=?",dlqId);
  log(eventId,"REPLAY_QUEUED","Simulated fault fixed; generation "+generation+" queued with the same event ID");
 }
 private Map<String,Object> locked(String id){
  var rows=db.queryForList("SELECT * FROM events WHERE id=? FOR UPDATE",id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Event not found");return rows.get(0);
 }
 private PostEvent fromRow(Map<String,Object> e){return new PostEvent((String)e.get("ID"),((Number)e.get("GENERATION")).intValue(),(String)e.get("TITLE"),(String)e.get("CONTENT"),(String)e.get("CHANNEL"),((Number)e.get("SCHEDULED_AT")).longValue());}
 public static class SimulatedFailure extends RuntimeException {public SimulatedFailure(String message){super(message);}}
 @Transactional(noRollbackFor=SimulatedFailure.class)
 public void process(PostEvent message){
  if(message==null || message.eventId()==null)throw new IllegalArgumentException("Missing event ID");
  Map<String,Object> e=locked(message.eventId());String id=message.eventId();
  if(db.queryForObject("SELECT COUNT(*) FROM processed_events WHERE event_id=?",Integer.class,id)>0){
   db.update("UPDATE events SET duplicates=duplicates+1,updated_at=? WHERE id=?",now(),id);
   log(id,"DUPLICATE_SKIPPED","Idempotency key exists: no second publication");return;
  }
  if(((Number)e.get("GENERATION")).intValue()!=message.generation()){
   log(id,"STALE_SKIPPED","Ignored delivery from an earlier replay generation");return;
  }
  int attempt=((Number)e.get("ATTEMPTS")).intValue()+1;
  db.update("UPDATE events SET attempts=?,updated_at=? WHERE id=?",attempt,now(),id);
  log(id,"ATTEMPT","Processing attempt "+attempt+" in generation "+message.generation());
  String mode=(String)e.get("FAILURE_MODE");
  if("ALWAYS".equals(mode) || ("TRANSIENT".equals(mode)&&attempt<=2)){
   String reason="ALWAYS".equals(mode)?"Simulated downstream service unavailable":"Simulated temporary network failure";
   db.update("UPDATE events SET status='RETRYING',last_error=?,updated_at=? WHERE id=?",reason,now(),id);
   log(id,"ATTEMPT_FAILED",reason);throw new SimulatedFailure(reason);
  }
  // The marker and the business effect commit atomically. Offset commits occur after this method returns.
  db.update("INSERT INTO processed_events VALUES(?,?)",id,now());
  db.update("INSERT INTO publications VALUES(?,?,?,?,?)",id,e.get("TITLE"),e.get("CONTENT"),e.get("CHANNEL"),now());
  db.update("UPDATE events SET status='PUBLISHED',last_error=NULL,updated_at=? WHERE id=?",now(),id);
  log(id,"PUBLISHED","Publication and idempotency marker committed together");
 }
 @Transactional public void deadLetter(String key,PostEvent event,String payload,String reason,String topic,int partition,long offset){
  if(db.queryForObject("SELECT COUNT(*) FROM dead_letters WHERE id=?",Integer.class,key)>0)return;
  String eventId=event==null?null:event.eventId();
  if(eventId!=null && db.queryForObject("SELECT COUNT(*) FROM events WHERE id=?",Integer.class,eventId)==0)eventId=null;
  db.update("INSERT INTO dead_letters VALUES(?,?,?,?,?,?,?,?,?,FALSE)",key,eventId,event==null?null:event.generation(),clip(payload,20000),clip(reason,1000),topic,partition,offset,now());
  if(eventId!=null)db.update("UPDATE events SET status='DLQ',last_error=?,updated_at=? WHERE id=? AND generation=? AND status<>'PUBLISHED'",clip(reason,500),now(),eventId,event.generation());
  log(eventId,"DEAD_LETTERED","Stored Kafka DLQ record from "+topic+" partition "+partition+" offset "+offset);
 }
 static String clip(String v,int max){if(v==null)return "";return v.length()>max?v.substring(0,max):v;}
}
