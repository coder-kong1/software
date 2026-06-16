PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS user_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    car_id TEXT NOT NULL UNIQUE COLLATE NOCASE,
    user_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    car_capacity REAL NOT NULL CHECK (car_capacity > 0),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charging_pile (
    id TEXT PRIMARY KEY,
    mode TEXT NOT NULL CHECK (mode IN ('FAST', 'SLOW')),
    status TEXT NOT NULL DEFAULT 'RUNNING'
        CHECK (status IN ('POWER_ON', 'RUNNING', 'STOPPED', 'FAULT')),
    power_kw REAL NOT NULL CHECK (power_kw > 0),
    queue_limit INTEGER NOT NULL DEFAULT 2 CHECK (queue_limit >= 0),
    total_charge_count INTEGER NOT NULL DEFAULT 0,
    total_charge_duration REAL NOT NULL DEFAULT 0,
    total_charge_amount REAL NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS charging_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    car_id TEXT NOT NULL,
    request_amount REAL NOT NULL CHECK (request_amount > 0),
    charged_amount REAL NOT NULL DEFAULT 0 CHECK (charged_amount >= 0),
    request_mode TEXT NOT NULL CHECK (request_mode IN ('FAST', 'SLOW')),
    state TEXT NOT NULL DEFAULT 'WAITING_AREA'
        CHECK (state IN ('WAITING_AREA', 'QUEUING', 'CHARGING', 'FINISHED', 'CANCELED')),
    queue_num TEXT,
    pile_id TEXT,
    request_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_time TEXT,
    end_time TEXT,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE,
    FOREIGN KEY (pile_id) REFERENCES charging_pile(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_charging_request_active_car
ON charging_request(car_id)
WHERE state IN ('WAITING_AREA', 'QUEUING', 'CHARGING');

CREATE INDEX IF NOT EXISTS idx_charging_request_state_mode_time
ON charging_request(state, request_mode, request_time);


CREATE TABLE IF NOT EXISTS scheduling_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER,
    car_id TEXT NOT NULL,
    request_mode TEXT CHECK (request_mode IN ('FAST', 'SLOW')),
    from_state TEXT,
    to_state TEXT,
    from_pile_id TEXT,
    to_pile_id TEXT,
    queue_num TEXT,
    strategy TEXT CHECK (strategy IN ('TIME_ORDER', 'PRIORITY')),
    reason TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES charging_request(id),
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE,
    FOREIGN KEY (from_pile_id) REFERENCES charging_pile(id),
    FOREIGN KEY (to_pile_id) REFERENCES charging_pile(id)
);

CREATE INDEX IF NOT EXISTS idx_scheduling_log_created_at
ON scheduling_log(created_at DESC, id DESC);
CREATE TABLE IF NOT EXISTS price_rule (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    peak_price REAL NOT NULL CHECK (peak_price >= 0),
    normal_price REAL NOT NULL CHECK (normal_price >= 0),
    valley_price REAL NOT NULL CHECK (valley_price >= 0),
    service_price REAL NOT NULL CHECK (service_price >= 0),
    fast_service_price REAL NOT NULL DEFAULT 1.0 CHECK (fast_service_price >= 0),
    slow_service_price REAL NOT NULL DEFAULT 0.8 CHECK (slow_service_price >= 0),
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app_setting (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bill (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_no TEXT NOT NULL UNIQUE,
    request_id INTEGER NOT NULL UNIQUE,
    car_id TEXT NOT NULL,
    pile_id TEXT NOT NULL,
    charge_amount REAL NOT NULL CHECK (charge_amount >= 0),
    charge_duration REAL NOT NULL CHECK (charge_duration >= 0),
    charge_fee REAL NOT NULL CHECK (charge_fee >= 0),
    service_fee REAL NOT NULL CHECK (service_fee >= 0),
    total_fee REAL NOT NULL CHECK (total_fee >= 0),
    status TEXT NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TEXT,
    FOREIGN KEY (request_id) REFERENCES charging_request(id),
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE,
    FOREIGN KEY (pile_id) REFERENCES charging_pile(id)
);

CREATE TABLE IF NOT EXISTS payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_no TEXT NOT NULL UNIQUE,
    car_id TEXT NOT NULL,
    amount REAL NOT NULL CHECK (amount >= 0),
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    paid_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_no) REFERENCES bill(bill_no),
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS abnormal_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    car_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    penalty_fee REAL NOT NULL DEFAULT 0 CHECK (penalty_fee >= 0),
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RESOLVED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TEXT,
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS penalty_bill (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_no TEXT NOT NULL UNIQUE,
    event_id INTEGER NOT NULL UNIQUE,
    car_id TEXT NOT NULL,
    amount REAL NOT NULL CHECK (amount >= 0),
    status TEXT NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TEXT,
    FOREIGN KEY (event_id) REFERENCES abnormal_event(id),
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS penalty_payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bill_no TEXT NOT NULL UNIQUE,
    car_id TEXT NOT NULL,
    amount REAL NOT NULL CHECK (amount >= 0),
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    paid_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_no) REFERENCES penalty_bill(bill_no),
    FOREIGN KEY (car_id) REFERENCES user_account(car_id) ON UPDATE CASCADE
);

