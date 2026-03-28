
-- cars: change INSERT, UPDATE, DELETE policies from public to authenticated
ALTER POLICY "Users can insert their own cars" ON cars TO authenticated;
ALTER POLICY "Users can update their own cars" ON cars TO authenticated;
ALTER POLICY "Users can delete their own cars" ON cars TO authenticated;

-- job_requests: change INSERT, UPDATE, DELETE policies from public to authenticated
ALTER POLICY "Users can insert their own job requests" ON job_requests TO authenticated;
ALTER POLICY "Users can update their own job requests" ON job_requests TO authenticated;
ALTER POLICY "Users can delete their own job requests" ON job_requests TO authenticated;

-- quotes: change INSERT, UPDATE policies from public to authenticated
ALTER POLICY "Garages can insert quotes" ON quotes TO authenticated;
ALTER POLICY "Garages can update their own quotes" ON quotes TO authenticated;

-- bookings: change INSERT policy from public to authenticated
ALTER POLICY "Users can insert bookings for accepted quotes" ON bookings TO authenticated;

-- reviews: change INSERT policy from public to authenticated
ALTER POLICY "Users can insert reviews for their bookings" ON reviews TO authenticated;

-- garages: change INSERT policy from public to authenticated
ALTER POLICY "Users can insert their garage" ON garages TO authenticated;
