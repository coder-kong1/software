package org.example.backend;

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
class BillingApiTests {

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
        jdbcTemplate.update("DELETE FROM charging_request");
        jdbcTemplate.update("DELETE FROM user_account");
        jdbcTemplate.update(
            """
            UPDATE charging_pile
            SET status = 'RUNNING', total_charge_count = 0,
                total_charge_duration = 0, total_charge_amount = 0
            """
        );
        jdbcTemplate.update(
            """
            UPDATE price_rule
            SET peak_price = 1.0, normal_price = 0.7,
                valley_price = 0.4, service_price = 0.8,
                fast_service_price = 1.0, slow_service_price = 0.8
            WHERE id = 1
            """
        );
    }

    @Test
    void completesChargingBillingAndPaymentLifecycle() throws Exception {
        createAccount("V10");

        mockMvc.perform(post("/api/charging/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"carId": "V10", "requestAmount": 20, "requestMode": "FAST"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.state").value("QUEUING"))
            .andExpect(jsonPath("$.data.pileId").value("F1"));

        mockMvc.perform(post("/api/charging/requests/V10/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pileId\": \"F1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.state").value("CHARGING"));

        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET start_time = datetime('now', '-1 hour')
            WHERE car_id = 'V10' AND state = 'CHARGING'
            """
        );

        mockMvc.perform(get("/api/charging/details/V10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.currentPosition").value("充电中"))
            .andExpect(jsonPath("$.data.chargedAmount").value(20.0))
            .andExpect(jsonPath("$.data.estimatedServiceFee").value(16.0));

        mockMvc.perform(post("/api/charging/requests/V10/end"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.carId").value("V10"))
            .andExpect(jsonPath("$.data.chargeAmount").value(20.0))
            .andExpect(jsonPath("$.data.status").value("UNPAID"));

        String billNo = jdbcTemplate.queryForObject(
            "SELECT bill_no FROM bill WHERE car_id = 'V10'",
            String.class
        );
        Double totalFee = jdbcTemplate.queryForObject(
            "SELECT total_fee FROM bill WHERE bill_no = ?",
            Double.class,
            billNo
        );

        mockMvc.perform(get("/api/charging/bills/V10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].billNo").value(billNo))
            .andExpect(jsonPath("$.data[0].startTime").isNotEmpty())
            .andExpect(jsonPath("$.data[0].endTime").isNotEmpty());

        mockMvc.perform(post("/api/charging/bills/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billNo": "%s", "carId": "V10", "amount": 0.01}
                    """.formatted(billNo)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/charging/bills/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billNo": "%s", "carId": "V10", "amount": %s}
                    """.formatted(billNo, totalFee)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.billNo").value(billNo))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get("/api/charging/payments/V10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].billNo").value(billNo));

        mockMvc.perform(post("/api/charging/bills/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billNo": "%s", "carId": "V10", "amount": %s}
                    """.formatted(billNo, totalFee)))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/reports/bills"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("PAID"));
    }

    @Test
    void readsAndUpdatesPriceRule() throws Exception {
        mockMvc.perform(get("/api/admin/price-rule"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.peakPrice").value(1.0))
            .andExpect(jsonPath("$.data.fastServicePrice").value(1.0))
            .andExpect(jsonPath("$.data.slowServicePrice").value(0.8));

        mockMvc.perform(put("/api/admin/price-rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "peakPrice": 1.2,
                      "normalPrice": 0.8,
                      "valleyPrice": 0.5,
                      "fastServicePrice": 1.1,
                      "slowServicePrice": 0.9
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.peakPrice").value(1.2))
            .andExpect(jsonPath("$.data.fastServicePrice").value(1.1))
            .andExpect(jsonPath("$.data.slowServicePrice").value(0.9));
    }

    @Test
    void returnsSameRealtimeChargedAmountAcrossUserAndAdminApis() throws Exception {
        createAccount("VPROGRESS");

        mockMvc.perform(post("/api/charging/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"carId": "VPROGRESS", "requestAmount": 40, "requestMode": "FAST"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/charging/requests/VPROGRESS/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pileId\": \"F1\"}"))
            .andExpect(status().isOk());

        jdbcTemplate.update(
            """
            UPDATE charging_request
            SET start_time = datetime('now', '-1 hour')
            WHERE car_id = 'VPROGRESS'
            """
        );

        mockMvc.perform(get("/api/charging/requests/VPROGRESS/state"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.chargedAmount").value(30.0));

        mockMvc.perform(get("/api/charging/details/VPROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.chargedAmount").value(30.0));

        mockMvc.perform(get("/api/admin/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.data.piles[?(@.pile.id == 'F1')].chargingCar.chargedAmount"
            ).value(30.0));

        mockMvc.perform(get("/api/admin/queues/F1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cars[0].chargedAmount").value(30.0));
    }

    private void createAccount(String carId) throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carId": "%s",
                      "userName": "计费测试用户",
                      "password": "123456",
                      "carCapacity": 100
                    }
                    """.formatted(carId)))
            .andExpect(status().isCreated());
    }
}
