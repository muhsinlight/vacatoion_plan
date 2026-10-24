-- Yakıt: aynı tür + geçerlilik günü tek satır.
DELETE FROM public.fuel_prices a
    USING public.fuel_prices b
WHERE a.fuel_type = b.fuel_type
  AND a.effective_from = b.effective_from
  AND a.id < b.id;

ALTER TABLE public.fuel_prices
    ADD CONSTRAINT uk_fuel_prices_type_effective UNIQUE (fuel_type, effective_from);

-- Gözlem: ingest tekil anahtarı (gün + isteğe bağlı hafta kovası).
ALTER TABLE public.price_observations
    ADD COLUMN IF NOT EXISTS observation_day DATE;

UPDATE public.price_observations
SET observation_day = (checked_at AT TIME ZONE 'UTC')::date
WHERE observation_day IS NULL;

ALTER TABLE public.price_observations
    ALTER COLUMN observation_day SET NOT NULL;

ALTER TABLE public.price_observations
    ADD COLUMN IF NOT EXISTS week_bucket DATE;

DELETE FROM public.price_observations a
    USING public.price_observations b
WHERE a.scope_type = b.scope_type
  AND a.scope_key = b.scope_key
  AND a.unit = b.unit
  AND a.observation_day = b.observation_day
  AND a.week_bucket IS NOT DISTINCT FROM b.week_bucket
  AND a.id < b.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_price_observations_ingest_key
    ON public.price_observations (scope_type, scope_key, unit, observation_day, week_bucket)
    NULLS NOT DISTINCT;
