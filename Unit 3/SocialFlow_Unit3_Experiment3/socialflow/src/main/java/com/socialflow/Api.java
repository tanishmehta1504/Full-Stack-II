package com.socialflow;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "bearerAuth")
public class Api {

  final Workspace w;
  final Security security;
  final BCryptPasswordEncoder passwords;

  public Api(Workspace w, Security security, BCryptPasswordEncoder passwords) {
    this.w = w;
    this.security = security;
    this.passwords = passwords;
  }

  record Login(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String password
  ) {}

  record Draft(
    @NotBlank @Size(max = 120) String title,
    @NotBlank @Size(max = 2800) String content,
    @NotBlank String channel
  ) {}

  record Schedule(@Min(0) long dueAt, boolean failDemo) {}

  record Comment(@NotBlank @Size(max = 800) String body) {}

  record Member(
    @NotBlank @Size(max = 80) String name,
    @Email @NotBlank @Size(max = 160) String email,
    @Size(min = 10, max = 72) @NotBlank String password,
    @Pattern(regexp = "EDITOR|VIEWER") @NotBlank String role
  ) {}

  @GetMapping("/health")
  public Map<String, String> health() {
    w.database().queryForObject("SELECT 1", Integer.class);
    return Map.of("status", "UP");
  }

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody Login input) {
    var list = w.rows(
      "SELECT * FROM members WHERE email=? AND active=TRUE",
      input.email().toLowerCase(Locale.ROOT)
    );
    if (
      list.isEmpty() ||
      !passwords.matches(input.password(), (String) list.get(0).get("password"))
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Invalid email or password"
    );
    var u = list.get(0);
    return Map.of(
      "token",
      security.token((String) u.get("id")),
      "user",
      w.member((String) u.get("id"))
    );
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication a) {
    return w.member(a.getName());
  }

  @GetMapping("/workspace")
  public Map<String, Object> snapshot() {
    return w.snapshot();
  }

  @PostMapping("/posts")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public Map<String, String> create(
    Authentication a,
    @Valid @RequestBody Draft d
  ) {
    return Map.of(
      "id",
      w.save(null, a.getName(), d.title(), d.content(), d.channel())
    );
  }

  @PutMapping("/posts/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public Map<String, String> edit(
    @PathVariable String id,
    Authentication a,
    @Valid @RequestBody Draft d
  ) {
    return Map.of(
      "id",
      w.save(id, a.getName(), d.title(), d.content(), d.channel())
    );
  }

  @PostMapping("/posts/{id}/schedule")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public Map<String, String> schedule(
    @PathVariable String id,
    Authentication a,
    @Valid @RequestBody Schedule s
  ) {
    w.schedule(id, a.getName(), s.dueAt(), s.failDemo());
    return Map.of("status", "SCHEDULED");
  }

  @PostMapping("/posts/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public Map<String, String> cancel(@PathVariable String id, Authentication a) {
    w.cancel(id, a.getName());
    return Map.of("status", "DRAFT");
  }

  @GetMapping("/posts/{id}/comments")
  public List<Map<String, Object>> comments(@PathVariable String id) {
    w.one("SELECT id FROM posts WHERE id=?", id);
    return w.rows(
      "SELECT c.*,m.name AS author FROM comments c JOIN members m ON c.author_id=m.id WHERE post_id=? ORDER BY created_at",
      id
    );
  }

  @PostMapping("/posts/{id}/comments")
  @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
  public Map<String, String> comment(
    @PathVariable String id,
    Authentication a,
    @Valid @RequestBody Comment c
  ) {
    w.one("SELECT id FROM posts WHERE id=?", id);
    w
      .database()
      .update(
        "INSERT INTO comments VALUES(?,?,?,?,?)",
        UUID.randomUUID().toString(),
        id,
        a.getName(),
        c.body().trim(),
        System.currentTimeMillis()
      );
    return Map.of("status", "ADDED");
  }

  @GetMapping("/members")
  public List<Map<String, Object>> members() {
    return w.rows(
      "SELECT id,name,email,role,active FROM members ORDER BY name"
    );
  }

  @PostMapping("/members")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, String> member(@Valid @RequestBody Member m) {
    String id = UUID.randomUUID().toString();
    w
      .database()
      .update(
        "INSERT INTO members VALUES(?,?,?,?,?,TRUE)",
        id,
        m.name().trim(),
        m.email().toLowerCase(Locale.ROOT),
        passwords.encode(m.password()),
        m.role()
      );
    w.audit("Team member added: " + m.name());
    return Map.of("id", id);
  }

  @PostMapping("/members/{id}/toggle")
  @PreAuthorize("hasRole('ADMIN')")
  public Map<String, String> toggle(@PathVariable String id) {
    var m = w.member(id);
    if (m.get("role").equals("ADMIN")) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "The bootstrap admin cannot be disabled"
    );
    w
      .database()
      .update(
        "UPDATE members SET active=? WHERE id=?",
        !Boolean.TRUE.equals(m.get("active")),
        id
      );
    return Map.of("status", "UPDATED");
  }
}
