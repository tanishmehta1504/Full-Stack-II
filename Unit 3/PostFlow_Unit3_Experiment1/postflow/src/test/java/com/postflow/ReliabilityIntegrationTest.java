package com.postflow;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:postflowtest;DB_CLOSE_DELAY=-1","app.retry-initial-ms=100","app.dispatch-ms=100",
 "logging.level.org.apache.kafka=ERROR","logging.level.kafka=ERROR","logging.level.org.springframework.kafka=ERROR"})
@EmbeddedKafka(partitions=3,topics={"postflow.posts.v1","postflow.posts.dlq.v1"},bootstrapServersProperty="spring.kafka.bootstrap-servers",kraft=true)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ReliabilityIntegrationTest {
 @Autowired TestRestTemplate http;@Autowired JdbcTemplate db;@Autowired KafkaTemplate<String,String> kafka;
 @Autowired EventService service;
 HttpEntity<?> entity(Object body){var headers=new HttpHeaders();headers.set("X-Lab-Request","postflow");headers.setContentType(MediaType.APPLICATION_JSON);return new HttpEntity<>(body,headers);}
 String create(String mode,long due){
  var r=http.postForEntity("/api/events",entity(Map.of("title","Test "+UUID.randomUUID(),"content","A classroom sample","channel","LinkedIn","failureMode",mode,"scheduledAt",due)),Map.class);
  assertEquals(HttpStatus.ACCEPTED,r.getStatusCode());return (String)r.getBody().get("id");
 }
 String status(String id){return db.queryForObject("SELECT status FROM events WHERE id=?",String.class,id);}
 int count(String sql,Object... params){return db.queryForObject(sql,Integer.class,params);}
 void waitFor(String id,String status){await().atMost(Duration.ofSeconds(40)).until(()->status.equals(status(id)));}
 @Test void normalPostPublishesOnceThroughRealKafka(){
  String id=create("NONE",0);waitFor(id,"PUBLISHED");
  assertEquals(1,count("SELECT COUNT(*) FROM publications WHERE event_id=?",id));
  assertEquals(1,count("SELECT COUNT(*) FROM processed_events WHERE event_id=?",id));
  assertEquals(1,count("SELECT attempts FROM events WHERE id=?",id));
 }
 @Test void transientFailureUsesExponentialBackoffAndEventuallySucceeds(){
  String id=create("TRANSIENT",0);waitFor(id,"PUBLISHED");
  assertEquals(3,count("SELECT attempts FROM events WHERE id=?",id));
  var timestamps=db.queryForList("SELECT created_at FROM activity WHERE event_id=? AND kind='ATTEMPT' ORDER BY created_at",Long.class,id);
  assertEquals(3,timestamps.size());assertTrue(timestamps.get(1)-timestamps.get(0)>=90);assertTrue(timestamps.get(2)-timestamps.get(1)>=180);
  assertEquals(0,count("SELECT COUNT(*) FROM dead_letters WHERE event_id=?",id));
 }
 @Test void exhaustedEventGoesToActualDlqAndCanBeReplayedOnce(){
  String id=create("ALWAYS",0);waitFor(id,"DLQ");
  assertEquals(4,count("SELECT attempts FROM events WHERE id=?",id));
  assertEquals(0,count("SELECT COUNT(*) FROM publications WHERE event_id=?",id));
  assertEquals(0,count("SELECT COUNT(*) FROM processed_events WHERE event_id=?",id));
  String dlq=db.queryForObject("SELECT id FROM dead_letters WHERE event_id=?",String.class,id);
  assertEquals("postflow.posts.v1",db.queryForObject("SELECT source_topic FROM dead_letters WHERE id=?",String.class,dlq));
  assertEquals(HttpStatus.ACCEPTED,http.postForEntity("/api/dead-letters/"+dlq+"/replay",entity(null),Map.class).getStatusCode());
  waitFor(id,"PUBLISHED");assertEquals(1,count("SELECT COUNT(*) FROM publications WHERE event_id=?",id));
  assertEquals(1,count("SELECT generation FROM events WHERE id=?",id));
  assertEquals(HttpStatus.CONFLICT,http.postForEntity("/api/dead-letters/"+dlq+"/replay",entity(null),Map.class).getStatusCode());
 }
 @Test void tenDuplicateDeliveriesDoNotRepeatTheBusinessEffect(){
  String id=create("NONE",0);waitFor(id,"PUBLISHED");
  for(int n=0;n<10;n++)assertEquals(HttpStatus.ACCEPTED,http.postForEntity("/api/events/"+id+"/duplicate",entity(null),Map.class).getStatusCode());
  await().atMost(Duration.ofSeconds(30)).until(()->count("SELECT duplicates FROM events WHERE id=?",id)>=10);
  assertEquals(1,count("SELECT COUNT(*) FROM publications WHERE event_id=?",id));
  assertEquals(1,count("SELECT COUNT(*) FROM processed_events WHERE event_id=?",id));
  assertEquals(1,count("SELECT attempts FROM events WHERE id=?",id));
 }
 @Test void scheduledPostStaysInOutboxUntilDue(){
  long due=System.currentTimeMillis()+2500;String id=create("NONE",due);
  assertEquals("SCHEDULED",status(id));
  assertEquals(1,count("SELECT COUNT(*) FROM outbox WHERE event_id=? AND sent_at IS NULL",id));
  waitFor(id,"PUBLISHED");
  long published=db.queryForObject("SELECT published_at FROM publications WHERE event_id=?",Long.class,id);
  assertTrue(published>=due);
 }
 @Test void malformedJsonIsQuarantinedWithoutBlockingHealthyMessages()throws Exception{
  String key="bad-"+UUID.randomUUID().toString().substring(0,20);
  kafka.send("postflow.posts.v1",key,"{broken-json").get(10,TimeUnit.SECONDS);
  await().atMost(Duration.ofSeconds(30)).until(()->count("SELECT COUNT(*) FROM dead_letters WHERE payload='{broken-json'")>0);
  String badId=db.queryForObject("SELECT id FROM dead_letters WHERE payload='{broken-json'",String.class);
  assertEquals(HttpStatus.CONFLICT,http.postForEntity("/api/dead-letters/"+badId+"/replay",entity(null),Map.class).getStatusCode());
  String healthy=create("NONE",0);waitFor(healthy,"PUBLISHED");
 }
 @Test void concurrentProcessingStillHasOnePublication()throws Exception{
  String id=create("NONE",System.currentTimeMillis()+60000);
  var event=new PostEvent(id,0,"test","content","X",0);
  var pool=java.util.concurrent.Executors.newFixedThreadPool(6);
  try{var tasks=new ArrayList<java.util.concurrent.Callable<Void>>();for(int i=0;i<6;i++)tasks.add(()->{service.process(event);return null;});
   for(var f:pool.invokeAll(tasks))f.get();
  }finally{pool.shutdownNow();}
  assertEquals(1,count("SELECT COUNT(*) FROM publications WHERE event_id=?",id));assertEquals(5,count("SELECT duplicates FROM events WHERE id=?",id));
 }
 @Test void invalidInputAndCrossOriginStyleMutationsAreRejected(){
  assertEquals(HttpStatus.FORBIDDEN,http.postForEntity("/api/events",Map.of(),Map.class).getStatusCode());
  assertEquals(HttpStatus.BAD_REQUEST,http.postForEntity("/api/events",entity(Map.of("title","","content","x","channel","X","failureMode","NONE")),Map.class).getStatusCode());
  assertEquals(HttpStatus.NOT_FOUND,http.getForEntity("/api/events/missing",Map.class).getStatusCode());
 }
 @Test void unacknowledgedSendRemainsInDurableOutbox(){
  var source=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:outboxoutage;DB_CLOSE_DELAY=-1","sa","");
  new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ClassPathResource("schema.sql")).execute(source);
  var localDb=new JdbcTemplate(source);var events=new EventService(localDb,new com.fasterxml.jackson.databind.ObjectMapper());
  String id=events.create("Outage test","Retained until Kafka ACK","X","NONE",0);
  Map<String,Object> config=new HashMap<>();
  config.put("bootstrap.servers","127.0.0.1:1");config.put("key.serializer",org.apache.kafka.common.serialization.StringSerializer.class);
  config.put("value.serializer",org.apache.kafka.common.serialization.StringSerializer.class);config.put("max.block.ms",300);
  config.put("delivery.timeout.ms",1000);config.put("request.timeout.ms",500);
  var factory=new org.springframework.kafka.core.DefaultKafkaProducerFactory<String,String>(config);
  try{new OutboxDispatcher(localDb,new KafkaTemplate<>(factory),events,"postflow.posts.v1").dispatch();}
  finally{factory.destroy();}
  assertEquals(1,localDb.queryForObject("SELECT COUNT(*) FROM outbox WHERE event_id=? AND sent_at IS NULL AND attempts=1",Integer.class,id));
  assertEquals("SCHEDULED",localDb.queryForObject("SELECT status FROM events WHERE id=?",String.class,id));
 }
 @Test void dashboardAndLiveSnapshotAreServed(){
  for(String path:List.of("/","/app.js","/style.css"))assertEquals(HttpStatus.OK,http.getForEntity(path,String.class).getStatusCode());
  var snapshot=http.getForEntity("/api/snapshot",Map.class);assertEquals(HttpStatus.OK,snapshot.getStatusCode());
  assertNotNull(snapshot.getBody().get("counts"));assertNotNull(snapshot.getBody().get("broker"));
 }
}
