
DROP POLICY IF EXISTS "Anyone can view approved garages" ON garages;

CREATE POLICY "Authenticated users can view approved garages"
ON garages
FOR SELECT
TO authenticated
USING ((is_approved = true) OR (auth.uid() = user_id));
