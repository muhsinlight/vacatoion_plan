-- Sık kullanılan FK ve filtre kolonları (changeSet 3). IF NOT EXISTS: tekrar çalıştırmada güvenli.
CREATE INDEX IF NOT EXISTS ix_trips_owner_id ON public.trips (owner_id);

CREATE INDEX IF NOT EXISTS ix_trip_budget_lines_trip_id ON public.trip_budget_lines (trip_id);

CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user_id ON public.refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS ix_price_observations_scope ON public.price_observations (scope_type, scope_key);

CREATE INDEX IF NOT EXISTS ix_fuel_prices_fuel_type_effective
    ON public.fuel_prices (fuel_type, effective_from DESC, checked_at DESC);
