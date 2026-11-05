-- Keep cancelled booking history while allowing the member to book the same class again.
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS uq_customer_session;
CREATE UNIQUE INDEX uq_bookings_active_customer_session
    ON bookings (customer_id, session_id)
    WHERE status <> 'CANCELLED';

-- A booking can have at most one attendance record. PostgreSQL permits multiple
-- NULL booking_id values for manual check-ins without a booking.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM attendance
        WHERE booking_id IS NOT NULL
        GROUP BY booking_id HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate attendance.booking_id rows exist; resolve them before V4_5';
    END IF;
END $$;

CREATE UNIQUE INDEX uq_attendance_booking_id
    ON attendance (booking_id)
    WHERE booking_id IS NOT NULL;

-- The existing PT service writes DEPLETED for a fully used package.
ALTER TABLE pt_packages DROP CONSTRAINT IF EXISTS pt_packages_status_check;
ALTER TABLE pt_packages ADD CONSTRAINT pt_packages_status_check
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'DEPLETED', 'EXPIRED', 'CANCELLED'));

-- A completed session cannot be completed again with a new reference.
CREATE FUNCTION prevent_repeat_pt_completion() RETURNS trigger AS $$
BEGIN
    IF NEW.status = 'COMPLETED' AND (NEW.completion_ref IS NULL OR BTRIM(NEW.completion_ref) = '') THEN
        RAISE EXCEPTION 'completion_ref is required when completing a PT session';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF OLD.status = 'COMPLETED'
            AND (NEW.status IS DISTINCT FROM OLD.status
                 OR NEW.completion_ref IS DISTINCT FROM OLD.completion_ref) THEN
            RAISE EXCEPTION 'Completed PT session cannot be completed again';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_repeat_pt_completion
    BEFORE INSERT OR UPDATE OF status, completion_ref ON pt_sessions
    FOR EACH ROW EXECUTE FUNCTION prevent_repeat_pt_completion();
