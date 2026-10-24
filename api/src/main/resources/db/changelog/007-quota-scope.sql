ALTER TABLE public.request_quota_windows
    ADD COLUMN IF NOT EXISTS scope_key VARCHAR(80) NOT NULL DEFAULT 'shared';

ALTER TABLE public.request_quota_windows
    DROP CONSTRAINT IF EXISTS uk_request_quota_bucket_window;

ALTER TABLE public.request_quota_windows
    ADD CONSTRAINT uk_request_quota_bucket_scope_window UNIQUE (bucket, scope_key, window_start);
