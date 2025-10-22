-- Create a function to check for booking conflicts
CREATE OR REPLACE FUNCTION public.check_booking_conflict()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_car_id uuid;
  v_conflict_count integer;
BEGIN
  -- Get the car_id for this booking
  SELECT jr.car_id INTO v_car_id
  FROM quotes q
  JOIN job_requests jr ON q.job_request_id = jr.id
  WHERE q.id = NEW.quote_id;

  -- Check if there's already a booking for this car at the same time
  -- Only check active bookings (not cancelled)
  SELECT COUNT(*) INTO v_conflict_count
  FROM bookings b
  JOIN quotes q ON b.quote_id = q.id
  JOIN job_requests jr ON q.job_request_id = jr.id
  WHERE jr.car_id = v_car_id
    AND b.scheduled_date = NEW.scheduled_date
    AND b.scheduled_time = NEW.scheduled_time
    AND b.status != 'cancelled'
    AND b.id != COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::uuid);

  IF v_conflict_count > 0 THEN
    RAISE EXCEPTION 'Mașina are deja o programare la această dată și oră. Te rugăm să alegi alt interval orar.';
  END IF;

  RETURN NEW;
END;
$$;

-- Create trigger for insert and update on bookings
DROP TRIGGER IF EXISTS prevent_booking_conflict ON public.bookings;
CREATE TRIGGER prevent_booking_conflict
  BEFORE INSERT OR UPDATE ON public.bookings
  FOR EACH ROW
  EXECUTE FUNCTION public.check_booking_conflict();