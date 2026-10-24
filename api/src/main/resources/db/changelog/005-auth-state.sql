CREATE TABLE IF NOT EXISTS public.auth_login_throttle (
    email VARCHAR(320) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    failure_count INT NOT NULL
);

CREATE TABLE IF NOT EXISTS public.auth_refresh_replay (
    previous_token_hash VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_auth_refresh_replay_user_id ON public.auth_refresh_replay (user_id);

CREATE INDEX IF NOT EXISTS ix_auth_refresh_replay_expires_at ON public.auth_refresh_replay (expires_at);
