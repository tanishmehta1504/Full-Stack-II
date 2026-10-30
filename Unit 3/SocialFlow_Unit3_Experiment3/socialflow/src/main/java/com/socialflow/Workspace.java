package com.socialflow;

import static org.springframework.http.HttpStatus.*;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Workspace {

  final JdbcTemplate db;

  public Workspace(JdbcTemplate db) {
    this.db = db;
  }

  public JdbcTemplate database() {
    return db;
  }

  public Map<String, Object> member(String id) {
    return one("SELECT id,name,email,role,active FROM members WHERE id=?", id);
  }

  public Map<String, Object> one(String sql, Object... args) {
    var rows = rows(sql, args);
    if (rows.isEmpty()) throw new ResponseStatusException(
      NOT_FOUND,
      "Record not found"
    );
    return rows.get(0);
  }

  public List<Map<String, Object>> rows(String sql, Object... args) {
    return db
      .queryForList(sql, args)
      .stream()
      .map(m -> {
        Map<String, Object> n = new LinkedHashMap<>();
        m.forEach((k, v) -> n.put(k.toLowerCase(Locale.ROOT), v));
        return n;
      })
      .toList();
  }

  public void audit(String msg) {
    db.update(
      "INSERT INTO audit VALUES(?,?,?)",
      UUID.randomUUID().toString(),
      msg,
      System.currentTimeMillis()
    );
  }

  public Map<String, Object> locked(String id, String actor) {
    var p = one("SELECT * FROM posts WHERE id=? FOR UPDATE", id);
    if (
      !member(actor).get("role").equals("ADMIN") &&
      !p.get("owner_id").equals(actor)
    ) throw new ResponseStatusException(
      FORBIDDEN,
      "Only the author or admin can change this post"
    );
    return p;
  }

  @Transactional
  public String save(
    String id,
    String actor,
    String title,
    String content,
    String channel
  ) {
    if (
      !Set.of("Demo LinkedIn", "Demo Instagram", "Demo X").contains(channel)
    ) throw new ResponseStatusException(BAD_REQUEST, "Choose a demo channel");
    if (id == null) {
      id = UUID.randomUUID().toString();
      db.update(
        "INSERT INTO posts(id,owner_id,title,content,channel,status,due_at,created_at) VALUES(?,?,?,?,?,'DRAFT',0,?)",
        id,
        actor,
        title.trim(),
        content.trim(),
        channel,
        System.currentTimeMillis()
      );
    } else {
      var p = locked(id, actor);
      if (!p.get("status").equals("DRAFT")) throw new ResponseStatusException(
        CONFLICT,
        "Only drafts can be edited"
      );
      db.update(
        "UPDATE posts SET title=?,content=?,channel=? WHERE id=?",
        title.trim(),
        content.trim(),
        channel,
        id
      );
    }
    audit("Draft saved: " + title.trim());
    return id;
  }

  @Transactional
  public void schedule(String id, String actor, long due, boolean fail) {
    var p = locked(id, actor);
    if (
      !Set.of("DRAFT", "FAILED").contains(p.get("status"))
    ) throw new ResponseStatusException(
      CONFLICT,
      "Post is already queued or published"
    );
    if (due < 0) throw new ResponseStatusException(
      BAD_REQUEST,
      "Invalid schedule"
    );
    if (
      fail && !member(actor).get("role").equals("ADMIN")
    ) throw new ResponseStatusException(
      FORBIDDEN,
      "Failure injection requires admin"
    );
    int generation = ((Number) p.get("generation")).intValue() + 1;
    db.update(
      "UPDATE posts SET status='SCHEDULED',due_at=?,generation=?,fail_demo=? WHERE id=?",
      Math.max(due, System.currentTimeMillis()),
      generation,
      fail,
      id
    );
    db.update(
      "INSERT INTO outbox(id,post_id,due_at,sent) VALUES(?,?,?,FALSE)",
      id + ":" + generation,
      id,
      Math.max(due, System.currentTimeMillis())
    );
    audit("Post queued: " + p.get("title"));
  }

  @Transactional
  public void cancel(String id, String actor) {
    var p = locked(id, actor);
    if (!p.get("status").equals("SCHEDULED")) throw new ResponseStatusException(
      CONFLICT,
      "Only scheduled posts can be cancelled"
    );
    db.update(
      "UPDATE posts SET status='DRAFT',generation=generation+1,due_at=0 WHERE id=?",
      id
    );
    db.update("UPDATE outbox SET sent=TRUE WHERE post_id=?", id);
    audit("Schedule cancelled: " + p.get("title"));
  }

  @Transactional
  public void publish(String event) {
    var bits = event.split(":");
    if (bits.length != 2) throw new IllegalArgumentException("Malformed event");
    int generation = Integer.parseInt(bits[1]);
    var found = rows("SELECT * FROM posts WHERE id=? FOR UPDATE", bits[0]);
    if (found.isEmpty()) return;
    var p = found.get(0);
    if (
      !p.get("status").equals("SCHEDULED") ||
      ((Number) p.get("generation")).intValue() != generation
    ) return;
    if (
      Boolean.TRUE.equals(p.get("fail_demo"))
    ) throw new IllegalStateException("Injected demo publisher failure");
    long now = System.currentTimeMillis();
    if (
      ((Number) p.get("due_at")).longValue() > now
    ) throw new IllegalStateException("Post not due");
    db.update(
      "INSERT INTO publications VALUES(?,?,?)",
      bits[0],
      now,
      p.get("content")
    );
    db.update(
      "UPDATE posts SET status='PUBLISHED',published_at=? WHERE id=?",
      now,
      bits[0]
    );
    audit("Published to demo feed: " + p.get("title"));
  }

  @Transactional
  public void failed(String event) {
    var bits = event.split(":");
    if (bits.length != 2) return;
    try {
      int g = Integer.parseInt(bits[1]);
      int n = db.update(
        "UPDATE posts SET status='FAILED' WHERE id=? AND generation=? AND status='SCHEDULED'",
        bits[0],
        g
      );
      if (n > 0) audit("Retry limit reached. Post moved to failed queue.");
    } catch (NumberFormatException ignored) {}
  }

  public Map<String, Object> snapshot() {
    return Map.of(
      "posts",
      rows(
        "SELECT p.*,m.name AS author FROM posts p JOIN members m ON p.owner_id=m.id ORDER BY p.created_at DESC LIMIT 500"
      ),
      "activity",
      rows("SELECT * FROM audit ORDER BY created_at DESC LIMIT 30"),
      "counts",
      rows("SELECT status,COUNT(*) AS total FROM posts GROUP BY status"),
      "daily",
      rows(
        "SELECT published_at FROM publications WHERE published_at>?",
        System.currentTimeMillis() - 7L * 86400000
      ),
      "publisher",
      "DEMO — publications stay inside this workspace"
    );
  }
}
