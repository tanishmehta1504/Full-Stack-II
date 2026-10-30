package com.releasepilot;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:api;DB_CLOSE_DELAY=-1") @AutoConfigureMockMvc
class ApiIntegrationTest {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;
 @Test void quoteContractFields()throws Exception{mvc.perform(get("/api/quote?quantity=2")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(998)).andExpect(jsonPath("$.currency").value("INR"));}
 @Test void orderPersists()throws Exception{int before=db.queryForObject("SELECT COUNT(*) FROM orders",Integer.class);mvc.perform(post("/api/orders").header("X-Lab-Request","releasepilot").contentType("application/json").content("{\"quantity\":3}")).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(1497));assertEquals(before+1,db.queryForObject("SELECT COUNT(*) FROM orders",Integer.class));}
 @Test void invalidOrderRejected()throws Exception{mvc.perform(post("/api/orders").header("X-Lab-Request","releasepilot").contentType("application/json").content("{\"quantity\":0}")).andExpect(status().isBadRequest());}
 @Test void mutationNeedsHeader()throws Exception{mvc.perform(post("/api/rollout").contentType("application/json").content("{\"percent\":100}")).andExpect(status().isForbidden());}
 @Test void dashboardAndHealth()throws Exception{mvc.perform(get("/")).andExpect(status().isOk());mvc.perform(get("/api/health")).andExpect(jsonPath("$.status").value("UP"));}
}
