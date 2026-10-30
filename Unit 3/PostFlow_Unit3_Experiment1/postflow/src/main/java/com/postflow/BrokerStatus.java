package com.postflow;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BrokerStatus {
 private final String bootstrap;private volatile boolean connected=false;private volatile long checked=0;
 public BrokerStatus(@Value("${spring.kafka.bootstrap-servers}") String bootstrap){this.bootstrap=bootstrap;}
 @Scheduled(fixedDelay=5000) public void check(){
  try(var client=AdminClient.create(Map.of("bootstrap.servers",bootstrap,"request.timeout.ms",2000,"default.api.timeout.ms",2000))){
   connected=!client.describeCluster().nodes().get(2500,TimeUnit.MILLISECONDS).isEmpty();
  }catch(Exception ex){connected=false;}finally{checked=System.currentTimeMillis();}
 }
 public Map<String,Object> status(){return Map.of("connected",connected,"checkedAt",checked,"bootstrap",bootstrap);}
}
