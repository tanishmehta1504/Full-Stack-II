package com.postflow;

import java.time.Instant;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
 public record CreatePost(@NotBlank @Size(max=120) String title,@NotBlank @Size(max=2000) String content,
  @NotNull @Pattern(regexp="LinkedIn|Instagram|X") String channel,
  @NotNull @Pattern(regexp="NONE|TRANSIENT|ALWAYS") String failureMode,Long scheduledAt){}
 private final JdbcTemplate db;private final EventService events;private final BrokerStatus broker;
 public ApiController(JdbcTemplate db,EventService events,BrokerStatus broker){this.db=db;this.events=events;this.broker=broker;}
 @GetMapping("/snapshot") public Map<String,Object> snapshot(){
  var counts=new LinkedHashMap<String,Object>();
  for(String status:List.of("SCHEDULED","QUEUED","RETRYING","PUBLISHED","DLQ"))counts.put(status,db.queryForObject("SELECT COUNT(*) FROM events WHERE status=?",Integer.class,status));
  counts.put("duplicates",db.queryForObject("SELECT COALESCE(SUM(duplicates),0) FROM events",Integer.class));
  counts.put("outbox",db.queryForObject("SELECT COUNT(*) FROM outbox WHERE sent_at IS NULL",Integer.class));
  counts.put("total",db.queryForObject("SELECT COUNT(*) FROM events",Integer.class));
  return Map.of("broker",broker.status(),"counts",counts,"events",rows("SELECT * FROM events ORDER BY created_at DESC LIMIT 200"),
   "deadLetters",rows("SELECT * FROM dead_letters ORDER BY created_at DESC LIMIT 100"),"publications",rows("SELECT * FROM publications ORDER BY published_at DESC LIMIT 100"),
   "activity",rows("SELECT * FROM activity ORDER BY created_at DESC LIMIT 100"));
 }
 @PostMapping("/events") @ResponseStatus(HttpStatus.ACCEPTED)
 public Map<String,String> create(@Valid @RequestBody CreatePost input){
  long due=input.scheduledAt()==null?EventService.now():input.scheduledAt();
  if(due>EventService.now()+366L*86400000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Schedule within the next year");
  String id=events.create(input.title().trim(),input.content().trim(),input.channel(),input.failureMode(),Math.max(EventService.now(),due));
  return Map.of("id",id,"message","Saved in the durable outbox; Kafka dispatch will occur when due");
 }
 @GetMapping("/events/{id}") public Map<String,Object> detail(@PathVariable String id){
  return Map.of("event",lower(events.event(id)),"activity",rows("SELECT * FROM activity WHERE event_id=? ORDER BY created_at",id),
   "outbox",rows("SELECT id,attempts,sent_at,next_try,last_error FROM outbox WHERE event_id=? ORDER BY due_at",id));
 }
 @PostMapping("/events/{id}/duplicate") @ResponseStatus(HttpStatus.ACCEPTED)
 public Map<String,String> duplicate(@PathVariable String id){events.duplicate(id);return Map.of("message","Duplicate queued with the same event ID");}
 @DeleteMapping("/events/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void delete(@PathVariable String id){events.delete(id);}
 @PostMapping("/dead-letters/{id}/replay") @ResponseStatus(HttpStatus.ACCEPTED)
 public Map<String,String> replay(@PathVariable String id){events.replay(id);return Map.of("message","Simulated fault removed; same event ID queued for replay");}
 private List<Map<String,Object>> rows(String sql,Object... args){return db.queryForList(sql,args).stream().map(ApiController::lower).toList();}
 private static Map<String,Object> lower(Map<String,Object> row){var result=new LinkedHashMap<String,Object>();row.forEach((k,v)->result.put(k.toLowerCase(Locale.ROOT),v));return result;}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> error(ResponseStatusException ex){return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",Objects.requireNonNullElse(ex.getReason(),"Request rejected")));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(Map.of("message",ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).findFirst().orElse("Invalid input")));}
}
