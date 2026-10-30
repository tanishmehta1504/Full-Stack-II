package com.socialflow;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Events {

  final Workspace w;
  final KafkaTemplate<String, String> kafka;

  public Events(Workspace w, KafkaTemplate<String, String> kafka) {
    this.w = w;
    this.kafka = kafka;
  }

  @Scheduled(fixedDelayString = "${app.dispatch-ms}")
  public void dispatch() {
    for (var row : w.rows(
      "SELECT * FROM outbox WHERE sent=FALSE AND due_at<=? ORDER BY due_at LIMIT 50",
      System.currentTimeMillis()
    )) {
      String id = (String) row.get("id");
      try {
        kafka
          .send("socialflow.posts", (String) row.get("post_id"), id)
          .get(6, java.util.concurrent.TimeUnit.SECONDS);
        w.database().update("UPDATE outbox SET sent=TRUE WHERE id=?", id);
      } catch (Exception e) {
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
          return;
        }
        break;
      }
    }
  }

  @KafkaListener(topics = "socialflow.posts")
  public void consume(String event) {
    w.publish(event);
  }

  @KafkaListener(
    topics = "socialflow.posts.dlq",
    containerFactory = "dlqFactory"
  )
  public void dead(String event) {
    w.failed(event);
  }
}
