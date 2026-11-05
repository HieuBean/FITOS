-- V3.3: Class Bookings & Attendance Records
-- Author: Member C (Operations & Bookings)

CREATE TABLE IF NOT EXISTS bookings (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT       NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    session_id    BIGINT       NOT NULL REFERENCES class_sessions(id) ON DELETE CASCADE,
    status        VARCHAR(50)  NOT NULL DEFAULT 'CONFIRMED'
                    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    booked_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  TIMESTAMP WITH TIME ZONE,
    cancel_reason VARCHAR(255),
    -- Spec 7.6: Ngăn chặn hội viên book trùng 2 lần trên cùng 1 session
    CONSTRAINT uq_customer_session UNIQUE (customer_id, session_id)
);

CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_session  ON bookings(session_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status   ON bookings(status);

-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS attendance (
    id               BIGSERIAL PRIMARY KEY,
    customer_id      BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    booking_id       BIGINT      REFERENCES bookings(id) ON DELETE SET NULL,
    class_session_id BIGINT      REFERENCES class_sessions(id) ON DELETE SET NULL,
    checkin_time     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    method           VARCHAR(50) NOT NULL DEFAULT 'DESK'
                       CHECK (method IN ('DESK', 'QR', 'MANUAL')),
    checked_in_by    BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    notes            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_attendance_customer     ON attendance(customer_id);
CREATE INDEX IF NOT EXISTS idx_attendance_checkin_time ON attendance(checkin_time DESC);
