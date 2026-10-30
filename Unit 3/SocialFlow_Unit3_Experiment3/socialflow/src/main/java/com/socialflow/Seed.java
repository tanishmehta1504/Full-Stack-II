package com.socialflow;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Seed implements CommandLineRunner {

  final Workspace w;
  final BCryptPasswordEncoder passwords;
  final boolean demo;
  final String admin;

  public Seed(
    Workspace w,
    BCryptPasswordEncoder passwords,
    @Value("${app.demo}") boolean demo,
    @Value("${app.admin-password}") String admin
  ) {
    this.w = w;
    this.passwords = passwords;
    this.demo = demo;
    this.admin = admin;
  }

  public void run(String... args) {
    if (
      !demo && (admin.equals("Admin@12345") || admin.length() < 12)
    ) throw new IllegalStateException(
      "Set a strong ADMIN_PASSWORD outside demo mode"
    );
    if (
      w
        .database()
        .queryForObject("SELECT COUNT(*) FROM members", Integer.class) >
      0
    ) return;
    add("Workspace Admin", "admin@socialflow.local", admin, "ADMIN");
    if (demo) {
      add("Aarav Shah", "editor@socialflow.local", "Editor@12345", "EDITOR");
      add("Maya Singh", "viewer@socialflow.local", "Viewer@12345", "VIEWER");
    }
    w.audit("Workspace ready. Connect your ideas to a publishing plan.");
  }

  void add(String name, String email, String password, String role) {
    w
      .database()
      .update(
        "INSERT INTO members VALUES(?,?,?,?,?,TRUE)",
        UUID.randomUUID().toString(),
        name,
        email,
        passwords.encode(password),
        role
      );
  }
}
