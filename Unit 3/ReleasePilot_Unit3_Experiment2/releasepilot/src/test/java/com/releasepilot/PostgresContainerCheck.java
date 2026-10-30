package com.releasepilot;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.junit.jupiter.api.Assertions.*;
@Testcontainers @SpringBootTest @AutoConfigureMockMvc
class PostgresContainerCheck {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 @Test void realPostgresPersistsOrder()throws Exception{mvc.perform(post("/api/orders").header("X-Lab-Request","releasepilot").contentType("application/json").content("{\"quantity\":2}")).andExpect(status().isCreated());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM orders",Integer.class));}
}
