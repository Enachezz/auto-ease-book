
-- 1. Drop the existing UPDATE policy on profiles
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;

-- 2. Create a new UPDATE policy that prevents changing user_type
CREATE POLICY "Users can update their own profile"
ON public.profiles
FOR UPDATE
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (
  auth.uid() = user_id
  AND user_type = (SELECT p.user_type FROM public.profiles p WHERE p.user_id = auth.uid())
);

-- 3. Drop the existing INSERT policy on profiles (handle_new_user trigger handles inserts)
DROP POLICY IF EXISTS "Users can insert their own profile" ON public.profiles;

-- 4. Recreate INSERT policy - user_type must be car_owner for self-registration
CREATE POLICY "Users can insert their own profile"
ON public.profiles
FOR INSERT
TO authenticated
WITH CHECK (
  auth.uid() = user_id
  AND user_type = 'car_owner'::user_type
);

-- 5. Drop the old garage SELECT policy on job_requests that relies on profile user_type
DROP POLICY IF EXISTS "Garages can view open job requests" ON public.job_requests;

-- 6. Create new policy that checks actual garage ownership
CREATE POLICY "Garages can view open job requests"
ON public.job_requests
FOR SELECT
TO authenticated
USING (
  status = 'open'
  AND EXISTS (
    SELECT 1 FROM public.garages
    WHERE garages.user_id = auth.uid()
      AND garages.is_approved = true
  )
);
