package com.postflow;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class PostFlowApplication {
 public static void main(String[] args) { SpringApplication.run(PostFlowApplication.class,args); }
}
