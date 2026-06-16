package org.example.backend.repository;

import org.example.backend.domain.PriceRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PriceRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public PriceRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PriceRule get() {
        return jdbcTemplate.queryForObject(
            """
            SELECT peak_price, normal_price, valley_price,
                   COALESCE(fast_service_price, service_price) AS fast_service_price,
                   COALESCE(slow_service_price, service_price) AS slow_service_price,
                   updated_at
            FROM price_rule
            WHERE id = 1
            """,
            (resultSet, rowNumber) -> new PriceRule(
                resultSet.getDouble("peak_price"),
                resultSet.getDouble("normal_price"),
                resultSet.getDouble("valley_price"),
                resultSet.getDouble("fast_service_price"),
                resultSet.getDouble("slow_service_price"),
                resultSet.getString("updated_at")
            )
        );
    }

    public void update(
        double peakPrice,
        double normalPrice,
        double valleyPrice,
        double fastServicePrice,
        double slowServicePrice
    ) {
        jdbcTemplate.update(
            """
            UPDATE price_rule
            SET peak_price = ?, normal_price = ?, valley_price = ?,
                fast_service_price = ?, slow_service_price = ?, service_price = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = 1
            """,
            peakPrice,
            normalPrice,
            valleyPrice,
            fastServicePrice,
            slowServicePrice,
            slowServicePrice
        );
    }
}
