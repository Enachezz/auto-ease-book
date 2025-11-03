-- Add DELETE policy for job_requests table
CREATE POLICY "Users can delete their own job requests"
ON public.job_requests
FOR DELETE
USING (auth.uid() = user_id);