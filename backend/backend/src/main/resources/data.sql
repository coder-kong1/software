INSERT OR IGNORE INTO charging_pile (id, mode, status, power_kw, queue_limit)
VALUES
    ('F1', 'FAST', 'RUNNING', 30.0, 2),
    ('F2', 'FAST', 'RUNNING', 30.0, 2),
    ('S1', 'SLOW', 'RUNNING', 10.0, 2),
    ('S2', 'SLOW', 'RUNNING', 10.0, 2),
    ('S3', 'SLOW', 'RUNNING', 10.0, 2);

UPDATE charging_pile
SET power_kw = 10.0, updated_at = CURRENT_TIMESTAMP
WHERE mode = 'SLOW' AND power_kw = 7.0;

INSERT OR IGNORE INTO price_rule
    (id, peak_price, normal_price, valley_price, service_price)
VALUES
    (1, 1.0, 0.7, 0.4, 0.8);

INSERT OR IGNORE INTO app_setting (setting_key, setting_value)
VALUES ('scheduling_strategy', 'TIME_ORDER');
