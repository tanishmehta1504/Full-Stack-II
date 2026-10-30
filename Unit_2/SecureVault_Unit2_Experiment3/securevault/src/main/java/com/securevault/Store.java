package com.securevault;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class Store {
    public record User(String id, String email, String name, String passwordHash, String role, long createdAt) {}
    private final JdbcTemplate db;
    private final RowMapper<User> userMapper = (rs, n) -> new User(rs.getString("id"), rs.getString("email"), rs.getString("display_name"), rs.getString("password_hash"), rs.getString("role"), rs.getLong("created_at"));
    public Store(JdbcTemplate db) { this.db = db; }
    public JdbcTemplate jdbc() { return db; }
    public Optional<User> byEmail(String email) { return db.query("SELECT * FROM app_users WHERE email=?", userMapper, email.toLowerCase(Locale.ROOT)).stream().findFirst(); }
    public Optional<User> byId(String id) { return db.query("SELECT * FROM app_users WHERE id=?", userMapper, id).stream().findFirst(); }
    public User createUser(String email, String name, String hash, String role) {
        User u = new User(UUID.randomUUID().toString(), email.toLowerCase(Locale.ROOT), name, hash, role, System.currentTimeMillis());
        db.update("INSERT INTO app_users VALUES(?,?,?,?,?,?)", u.id(), u.email(), u.name(), u.passwordHash(), u.role(), u.createdAt());
        return u;
    }
    public Map<String,Object> publicUser(User u) { return Map.of("id",u.id(),"email",u.email(),"name",u.name(),"role",u.role(),"createdAt",u.createdAt()); }
    public List<Map<String,Object>> users() { return db.query("SELECT * FROM app_users ORDER BY created_at", userMapper).stream().map(this::publicUser).toList(); }
    public void audit(String actor, String action, String detail) {
        db.update("INSERT INTO audit_events VALUES(?,?,?,?,?)", UUID.randomUUID().toString(), actor, action, detail, System.currentTimeMillis());
    }
    public List<Map<String,Object>> events(String actor, boolean admin) {
        String sql = "SELECT * FROM audit_events" + (admin ? "" : " WHERE actor=?") + " ORDER BY created_at DESC LIMIT 100";
        RowMapper<Map<String,Object>> mapper = (r,n) -> Map.of("id",r.getString("id"),"actor",r.getString("actor"),"action",r.getString("action"),"detail",r.getString("detail"),"createdAt",r.getLong("created_at"));
        return admin ? db.query(sql,mapper) : db.query(sql,mapper,actor);
    }
}
