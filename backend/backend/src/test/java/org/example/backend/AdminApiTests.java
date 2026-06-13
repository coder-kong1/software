package org.example.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminApiTests {

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
    }

    @Test
    void managesPileStateAndReturnsStationSnapshot() throws Exception {
        mockMvc.perform(get("/api/admin/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.piles.length()").value(5))
            .andExpect(jsonPath("$.data.piles[0].pile.id").isNotEmpty());

        mockMvc.perform(post("/api/admin/piles/F1/fault"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.piles[0].pile.status").value("FAULT"));

        mockMvc.perform(post("/api/admin/piles/F1/fault"))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/admin/piles/F1/recover"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/piles/F1/power-off"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/piles/F1/power-on"))
            .andExpect(status().isOk());
    }

    @Test
    void returnsPileQueueWithCapacityAndEstimatedWaitTime() throws Exception {
        createAccount("VQUEUE1");
        createAccount("VQUEUE2");
        createChargingRequest("VQUEUE1");
        createChargingRequest("VQUEUE2");

        mockMvc.perform(get("/api/admin/queues/F1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pile.id").value("F1"))
            .andExpect(jsonPath("$.data.cars.length()").value(1))
            .andExpect(jsonPath("$.data.cars[0].carCapacity").value(100))
            .andExpect(jsonPath("$.data.cars[0].requestAmount").value(20))
            .andExpect(jsonPath("$.data.cars[0].estimatedWaitHours").value(0));
    }

    @Test
    void createsListsAndResolvesAbnormalEvents() throws Exception {
        createAccount("VADMIN");

        mockMvc.perform(post("/api/admin/abnormal-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carId": "VADMIN",
                      "eventType": "QUEUE_JUMP",
                      "description": "恶意插队",
                      "penaltyFee": 80
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.carId").value("VADMIN"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.penaltyFee").value(80));

        mockMvc.perform(get("/api/charging/abnormal-events/VADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].penaltyStatus").value("UNPAID"))
            .andExpect(jsonPath("$.data[0].notification").value(
                org.hamcrest.Matchers.containsString("待支付罚款")
            ));

        String penaltyBillNo = jdbcTemplate.queryForObject(
            "SELECT bill_no FROM penalty_bill WHERE car_id = 'VADMIN'",
            String.class
        );

        mockMvc.perform(get("/api/charging/bills/VADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].billNo").value(penaltyBillNo))
            .andExpect(jsonPath("$.data[0].billType").value("PENALTY"))
            .andExpect(jsonPath("$.data[0].totalFee").value(80))
            .andExpect(jsonPath("$.data[0].status").value("UNPAID"));

        mockMvc.perform(post("/api/charging/bills/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"billNo": "%s", "carId": "VADMIN", "amount": 80}
                    """.formatted(penaltyBillNo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.billNo").value(penaltyBillNo))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get("/api/charging/payments/VADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].billNo").value(penaltyBillNo));

        Long eventId = jdbcTemplate.queryForObject(
            "SELECT id FROM abnormal_event WHERE car_id = 'VADMIN'",
            Long.class
        );

        mockMvc.perform(get("/api/admin/abnormal-events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(eventId))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        mockMvc.perform(post("/api/admin/abnormal-events/{id}/resolve", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RESOLVED"))
            .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        mockMvc.perform(get("/api/charging/abnormal-events/VADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("RESOLVED"))
            .andExpect(jsonPath("$.data[0].penaltyStatus").value("PAID"))
            .andExpect(jsonPath("$.data[0].notification").value(
                org.hamcrest.Matchers.containsString("已完成")
            ));

        mockMvc.perform(post("/api/admin/abnormal-events/{id}/resolve", eventId))
            .andExpect(status().isConflict());
    }

    @Test
    void returnsOperationReportSummary() throws Exception {
        createAccount("VREPORT");
        jdbcTemplate.update(
            """
            INSERT INTO charging_request (
                car_id, request_amount, charged_amount, request_mode, state,
                pile_id, start_time, end_time
            )
            VALUES ('VREPORT', 20, 20, 'FAST', 'FINISHED', 'F1',
                    datetime('now', '-1 hour'), CURRENT_TIMESTAMP)
            """
        );
        Long requestId = jdbcTemplate.queryForObject(
            "SELECT id FROM charging_request WHERE car_id = 'VREPORT'",
            Long.class
        );
        jdbcTemplate.update(
            """
            INSERT INTO bill (
                bill_no, request_id, car_id, pile_id, charge_amount,
                charge_duration, charge_fee, service_fee, total_fee, status, paid_at
            )
            VALUES ('BREPORT', ?, 'VREPORT', 'F1', 20, 1, 14, 16, 30, 'PAID',
                    CURRENT_TIMESTAMP)
            """,
            requestId
        );
        jdbcTemplate.update(
            """
            INSERT INTO abnormal_event (
                car_id, event_type, description, penalty_fee, status
            )
            VALUES ('VREPORT', 'OVERSTAY', '充完未驶离', 20, 'PENDING')
            """
        );

        mockMvc.perform(get("/api/admin/reports/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.billCount").value(1))
            .andExpect(jsonPath("$.data.paidBillCount").value(1))
            .andExpect(jsonPath("$.data.totalChargeAmount").value(20))
            .andExpect(jsonPath("$.data.totalRevenue").value(30))
            .andExpect(jsonPath("$.data.pendingAbnormalCount").value(1));
    }

    private void createAccount(String carId) throws Exception {
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "carId": "%s",
                      "userName": "管理员测试用户",
                      "password": "123456",
                      "carCapacity": 100
                    }
                    """.formatted(carId)))
            .andExpect(status().isCreated());
    }

    private void createChargingRequest(String carId) throws Exception {
        mockMvc.perform(post("/api/charging/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"carId": "%s", "requestAmount": 20, "requestMode": "FAST"}
                    """.formatted(carId)))
            .andExpect(status().isCreated());
    }
}
