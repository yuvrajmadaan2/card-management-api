CREATE TABLE card_programs (
    id BIGSERIAL PRIMARY KEY,
    program_id VARCHAR(255) NOT NULL UNIQUE,
    partner_id VARCHAR(255) NOT NULL,
    program_name VARCHAR(255) NOT NULL,
    program_type VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE cards (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(255) NOT NULL UNIQUE,
    partner_id VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    card_program_type VARCHAR(255),
    card_type VARCHAR(255),
    card_program_id VARCHAR(255),
    card_number VARCHAR(255),
    expiry_date VARCHAR(255),
    card_status VARCHAR(255),
    name_on_card VARCHAR(255),
    customer_id VARCHAR(255),
    issued_date VARCHAR(255)
);

CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    card_number VARCHAR(255),
    expiry_date VARCHAR(255),
    card_id VARCHAR(255),
    reference_id VARCHAR(255),
    response_code VARCHAR(255),
    response_desc VARCHAR(255),
    CONSTRAINT uk_idempotency_partner_key
        UNIQUE (partner_id, idempotency_key)
);

CREATE TABLE transaction_controls (
    id BIGSERIAL PRIMARY KEY,
    card_id VARCHAR(255) NOT NULL,
    channel_type VARCHAR(255) NOT NULL,
    allowed BOOLEAN NOT NULL,
    editable BOOLEAN NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_transaction_control_card_channel
        UNIQUE (card_id, channel_type)
);