package com.socialflow;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:socialtest;DB_CLOSE_DELAY=-1}",
    "app.dispatch-ms=100",
    "app.retry-ms=100",
    "app.jwt-secret=c29jaWFsZmxvdy10ZXN0LW9ubHktcmFuZG9tLWtleS1kb250LXVzZQ==",
    "logging.level.org.apache.kafka=ERROR",
    "logging.level.kafka=ERROR",
    "logging.level.org.springframework.kafka=ERROR",
  }
)
@EmbeddedKafka(
  partitions = 3,
  topics = { "socialflow.posts", "socialflow.posts.dlq" },
  bootstrapServersProperty = "spring.kafka.bootstrap-servers",
  kraft = true
)
@DirtiesContext
class WorkflowTest {

  @Autowired
  TestRestTemplate http;

  @Autowired
  Workspace w;

  @Autowired
  KafkaTemplate<String, String> kafka;

  @Autowired
  Security security;

  String token(String email, String pass) {
    var r = req(
      "/api/login",
      HttpMethod.POST,
      null,
      Map.of("email", email, "password", pass)
    );
    assertEquals(200, r.getStatusCode().value());
    return (String) r.getBody().get("token");
  }

  String admin() {
    return token("admin@socialflow.local", "Admin@12345");
  }

  String editor() {
    return token("editor@socialflow.local", "Editor@12345");
  }

  String viewer() {
    return token("viewer@socialflow.local", "Viewer@12345");
  }

  ResponseEntity<Map> req(
    String url,
    HttpMethod method,
    String token,
    Object body
  ) {
    var h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) h.setBearerAuth(token);
    return http.exchange(url, method, new HttpEntity<>(body, h), Map.class);
  }

  Map<String, Object> draft() {
    return Map.of(
      "title",
      "Launch " + UUID.randomUUID(),
      "content",
      "A new idea for our community.",
      "channel",
      "Demo LinkedIn"
    );
  }

  String create(String t) {
    var r = req("/api/posts", HttpMethod.POST, t, draft());
    assertEquals(200, r.getStatusCode().value());
    return (String) r.getBody().get("id");
  }

  void queue(String id, String token, long due, boolean fail) {
    assertEquals(
      200,
      req(
        "/api/posts/" + id + "/schedule",
        HttpMethod.POST,
        token,
        Map.of("dueAt", due, "failDemo", fail)
      )
        .getStatusCode()
        .value()
    );
  }

  String status(String id) {
    return (String) w
      .one("SELECT status FROM posts WHERE id=?", id)
      .get("status");
  }

  void waitStatus(String id, String value) {
    await()
      .atMost(Duration.ofSeconds(35))
      .until(() -> value.equals(status(id)));
  }

  @Test
  void loginValidationAndJwtProtection() {
    assertEquals(
      200,
      req("/api/health", HttpMethod.GET, null, null).getStatusCode().value()
    );
    assertEquals(
      401,
      req("/api/workspace", HttpMethod.GET, null, null).getStatusCode().value()
    );
    assertEquals(
      401,
      req("/api/workspace", HttpMethod.GET, "broken", null)
        .getStatusCode()
        .value()
    );
    assertEquals(
      401,
      req(
        "/api/login",
        HttpMethod.POST,
        null,
        Map.of("email", "nobody@example.com", "password", "wrong")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      401,
      req(
        "/api/login",
        HttpMethod.POST,
        null,
        Map.of("email", "admin@socialflow.local", "password", "wrong")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      400,
      req(
        "/api/login",
        HttpMethod.POST,
        null,
        Map.of("email", "invalid", "password", "")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      "ADMIN",
      req("/api/me", HttpMethod.GET, admin(), null).getBody().get("role")
    );
  }

  @Test
  void draftSchedulingPublishingAndIdempotency() throws Exception {
    String t = editor(),
      id = create(t);
    assertEquals("DRAFT", status(id));
    assertEquals(
      200,
      req(
        "/api/posts/" + id,
        HttpMethod.PUT,
        t,
        Map.of(
          "title",
          "Edited launch",
          "content",
          "Updated content",
          "channel",
          "Demo X"
        )
      )
        .getStatusCode()
        .value()
    );
    long due = System.currentTimeMillis() + 2200;
    queue(id, t, due, false);
    assertEquals("SCHEDULED", status(id));
    waitStatus(id, "PUBLISHED");
    long published = ((Number) w
        .one("SELECT published_at FROM posts WHERE id=?", id)
        .get("published_at")).longValue();
    assertTrue(published >= due);
    for (int n = 0; n < 4; n++) kafka
      .send("socialflow.posts", id, id + ":1")
      .get();
    w.publish(id + ":1");
    assertEquals(
      1,
      w
        .database()
        .queryForObject(
          "SELECT COUNT(*) FROM publications WHERE post_id=?",
          Integer.class,
          id
        )
    );
    assertEquals(
      409,
      req("/api/posts/" + id, HttpMethod.PUT, t, draft())
        .getStatusCode()
        .value()
    );
    assertEquals(
      409,
      req(
        "/api/posts/" + id + "/schedule",
        HttpMethod.POST,
        t,
        Map.of("dueAt", 0)
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      409,
      req("/api/posts/" + id + "/cancel", HttpMethod.POST, t, null)
        .getStatusCode()
        .value()
    );
  }

  @Test
  void failureGoesThroughKafkaDlqThenRetry() {
    String t = admin(),
      id = create(t);
    queue(id, t, 0, true);
    waitStatus(id, "FAILED");
    assertEquals(
      0,
      w
        .database()
        .queryForObject(
          "SELECT COUNT(*) FROM publications WHERE post_id=?",
          Integer.class,
          id
        )
    );
    queue(id, t, 0, false);
    waitStatus(id, "PUBLISHED");
    w.failed(id + ":1");
    assertEquals("PUBLISHED", status(id));
  }

  @Test
  void cancellationInvalidatesStaleMessages() {
    String t = editor(),
      id = create(t);
    queue(id, t, System.currentTimeMillis() + 60000, false);
    assertEquals(
      200,
      req("/api/posts/" + id + "/cancel", HttpMethod.POST, t, null)
        .getStatusCode()
        .value()
    );
    w.publish(id + ":1");
    assertEquals("DRAFT", status(id));
    assertEquals(
      409,
      req("/api/posts/" + id + "/cancel", HttpMethod.POST, t, null)
        .getStatusCode()
        .value()
    );
  }

  @Test
  void viewerCannotWriteAndEditorsCannotEditOthers() {
    String a = admin(),
      e = editor(),
      v = viewer(),
      id = create(a);
    assertEquals(
      403,
      req("/api/posts", HttpMethod.POST, v, draft()).getStatusCode().value()
    );
    assertEquals(
      403,
      req("/api/posts/" + id, HttpMethod.PUT, e, draft())
        .getStatusCode()
        .value()
    );
    assertEquals(
      403,
      req(
        "/api/members",
        HttpMethod.POST,
        e,
        Map.of(
          "name",
          "Blocked",
          "email",
          "blocked@example.com",
          "password",
          "Password@12345",
          "role",
          "VIEWER"
        )
      )
        .getStatusCode()
        .value()
    );
    String own = create(e);
    assertEquals(
      403,
      req(
        "/api/posts/" + own + "/schedule",
        HttpMethod.POST,
        e,
        Map.of("dueAt", 0, "failDemo", true)
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      200,
      req("/api/workspace", HttpMethod.GET, v, null).getStatusCode().value()
    );
  }

  @Test
  void teamManagementAndRevocation() {
    String a = admin(),
      email = "new-" + UUID.randomUUID() + "@example.com";
    var body = Map.of(
      "name",
      "New Editor",
      "email",
      email,
      "password",
      "Password@12345",
      "role",
      "EDITOR"
    );
    var created = req("/api/members", HttpMethod.POST, a, body);
    assertEquals(200, created.getStatusCode().value());
    String id = (String) created.getBody().get("id"),
      t = token(email, "Password@12345");
    assertEquals(
      409,
      req("/api/members", HttpMethod.POST, a, body).getStatusCode().value()
    );
    assertEquals(
      200,
      req("/api/members/" + id + "/toggle", HttpMethod.POST, a, null)
        .getStatusCode()
        .value()
    );
    assertEquals(
      401,
      req("/api/me", HttpMethod.GET, t, null).getStatusCode().value()
    );
    assertEquals(
      200,
      req("/api/members/" + id + "/toggle", HttpMethod.POST, a, null)
        .getStatusCode()
        .value()
    );
    String aid = (String) req("/api/me", HttpMethod.GET, a, null)
      .getBody()
      .get("id");
    assertEquals(
      409,
      req("/api/members/" + aid + "/toggle", HttpMethod.POST, a, null)
        .getStatusCode()
        .value()
    );
    var h = new HttpHeaders();
    h.setBearerAuth(a);
    assertEquals(
      200,
      http
        .exchange(
          "/api/members",
          HttpMethod.GET,
          new HttpEntity<>(h),
          List.class
        )
        .getStatusCode()
        .value()
    );
    assertEquals(
      400,
      req(
        "/api/members",
        HttpMethod.POST,
        a,
        Map.of(
          "name",
          "Test",
          "email",
          "x@x.com",
          "password",
          "short",
          "role",
          "ADMIN"
        )
      )
        .getStatusCode()
        .value()
    );
  }

  @Test
  void commentsAreSharedAndValidated() {
    String a = admin(),
      e = editor(),
      id = create(a);
    assertEquals(
      200,
      req(
        "/api/posts/" + id + "/comments",
        HttpMethod.POST,
        e,
        Map.of("body", "Ready for review!")
      )
        .getStatusCode()
        .value()
    );
    var h = new HttpHeaders();
    h.setBearerAuth(e);
    var r = http.exchange(
      "/api/posts/" + id + "/comments",
      HttpMethod.GET,
      new HttpEntity<>(h),
      List.class
    );
    assertEquals(1, r.getBody().size());
    assertEquals(
      400,
      req(
        "/api/posts/" + id + "/comments",
        HttpMethod.POST,
        e,
        Map.of("body", "")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      404,
      req(
        "/api/posts/missing/comments",
        HttpMethod.POST,
        e,
        Map.of("body", "hi")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      403,
      req(
        "/api/posts/" + id + "/comments",
        HttpMethod.POST,
        viewer(),
        Map.of("body", "hi")
      )
        .getStatusCode()
        .value()
    );
  }

  @Test
  void invalidInputsAndUnknownPosts() {
    String a = admin();
    assertEquals(
      400,
      req(
        "/api/posts",
        HttpMethod.POST,
        a,
        Map.of("title", "", "content", "", "channel", "Demo X")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      400,
      req(
        "/api/posts",
        HttpMethod.POST,
        a,
        Map.of("title", "Hi", "content", "Content", "channel", "Actual X")
      )
        .getStatusCode()
        .value()
    );
    assertEquals(
      404,
      req("/api/posts/missing", HttpMethod.PUT, a, draft())
        .getStatusCode()
        .value()
    );
    String id = create(a);
    assertEquals(
      400,
      req(
        "/api/posts/" + id + "/schedule",
        HttpMethod.POST,
        a,
        Map.of("dueAt", -1)
      )
        .getStatusCode()
        .value()
    );
    assertThrows(IllegalArgumentException.class, () -> w.publish("bad"));
    assertThrows(NumberFormatException.class, () -> w.publish(id + ":oops"));
    w.failed("bad");
    w.failed(id + ":oops");
    w.publish("missing:1");
  }

  @Test
  void prematureAndStaleEventsCannotPublish() {
    String a = admin(),
      id = create(a);
    queue(id, a, System.currentTimeMillis() + 60000, false);
    assertThrows(IllegalStateException.class, () -> w.publish(id + ":1"));
    w.publish(id + ":0");
    assertEquals("SCHEDULED", status(id));
  }

  @Test
  void docsAndDashboardAreAvailable() {
    assertEquals(
      200,
      http.getForEntity("/", String.class).getStatusCode().value()
    );
    var r = http.getForEntity("/v3/api-docs", Map.class);
    assertEquals(200, r.getStatusCode().value());
    assertNotNull(r.getBody().get("paths"));
    assertNotNull(
      req("/api/workspace", HttpMethod.GET, admin(), null)
        .getBody()
        .get("daily")
    );
  }
}
