package org.example.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:./build/test-charging.db")
class AccountAndChargingApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBusinessData() {
        jdbcTemplate.update("DELETE FROM penalty_payment");
        jdbcTemplate.update("DELETE FROM penalty_bill");
        jdbcTemplate.update("DELETE FROM payment");
        jdbcTemplate.update("DELETE FROM bill");
        jdbcTemplate.update("DELETE FROM abnormal_event");
        jdbcTemplate.update("DELETE FROM scheduling_log");
        jdbcTemplate.update("DELETE FROM charging_request");
        jdbcTemplate.update("DELETE FROM user_account");
    }

    @Test
    void completesAccountAndChargingRequestLifecycle() throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carId": "v1",
                      "userName": "测试用户",
                      "password": "123456",
                      "carCapacity": 100
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.carId").value("V1"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"account": "V1", "password": "123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userName").value("测试用户"));

        jdbcTemplate.update("UPDATE charging_pile SET status = 'STOPPED'");

        mockMvc.perform(post("/api/charging/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"carId": "V1", "requestAmount": 40, "requestMode": "SLOW"}
            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.state").value("WAITING_AREA"))
            .andExpect(jsonPath("$.data.queueNum").isNotEmpty());

        mockMvc.perform(put("/api/charging/requests/V1/amount")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestAmount").value(50));

        mockMvc.perform(put("/api/charging/requests/V1/mode")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\": \"FAST\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.requestMode").value("FAST"));

        mockMvc.perform(get("/api/charging/requests/V1/state"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.carId").value("V1"));

        mockMvc.perform(delete("/api/charging/requests/V1"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/charging/requests/V1/state"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsDuplicateAccountInvalidLoginAndExcessAmount() throws Exception {
        String account = """
            {
              "carId": "V2",
              "userName": "用户2",
              "password": "123456",
              "carCapacity": 60
            }
            """;

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(account))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(account))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/accounts/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"carId\": \"V2\", \"password\": \"wrong-password\"}"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/charging/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"carId": "V2", "requestAmount": 61, "requestMode": "FAST"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
