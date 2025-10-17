-- Create storage bucket for car documents
INSERT INTO storage.buckets (id, name, public)
VALUES ('car-documents', 'car-documents', false);

-- Create policies for car document uploads
CREATE POLICY "Users can view their own car documents"
ON storage.objects
FOR SELECT
USING (
  bucket_id = 'car-documents' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can upload their own car documents"
ON storage.objects
FOR INSERT
WITH CHECK (
  bucket_id = 'car-documents' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can update their own car documents"
ON storage.objects
FOR UPDATE
USING (
  bucket_id = 'car-documents' AND
  (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can delete their own car documents"
ON storage.objects
FOR DELETE
USING (
  bucket_id = 'car-documents' AND
  (storage.foldername(name))[1] = auth.uid()::text
);