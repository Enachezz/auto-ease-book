
DROP POLICY IF EXISTS "Garage owners can update their garage" ON public.garages;

CREATE POLICY "Garage owners can update their garage"
ON public.garages
FOR UPDATE
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (
  auth.uid() = user_id
  AND is_approved = (SELECT g.is_approved FROM public.garages g WHERE g.id = garages.id)
);
