package com.releasepilot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @EnableScheduling
public class ReleasePilotApplication {public static void main(String[] args)throws Exception{
 if(args.length==1&&args[0].equals("--healthcheck")){
  String port=System.getenv().getOrDefault("PORT","8082");
  try{var response=java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/health")).timeout(java.time.Duration.ofSeconds(3)).GET().build(),java.net.http.HttpResponse.BodyHandlers.discarding());System.exit(response.statusCode()==200?0:1);}catch(Exception e){System.exit(1);}return;
 }
 SpringApplication.run(ReleasePilotApplication.class,args);
}}
