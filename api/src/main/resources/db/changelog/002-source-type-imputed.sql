-- ddl-auto: update CHECK kısıtını güncellemediği için eski veritabanlarında IMPUTED eksik kaldı.
ALTER TABLE public.catalog_activities DROP CONSTRAINT IF EXISTS catalog_activities_source_type_check;
ALTER TABLE public.catalog_activities ADD CONSTRAINT catalog_activities_source_type_check
    CHECK (source_type IN ('OFFICIAL_API', 'SCRAPE', 'ESTIMATE', 'MANUAL', 'IMPUTED'));

ALTER TABLE public.price_records DROP CONSTRAINT IF EXISTS price_records_source_type_check;
ALTER TABLE public.price_records ADD CONSTRAINT price_records_source_type_check
    CHECK (source_type IN ('OFFICIAL_API', 'SCRAPE', 'ESTIMATE', 'MANUAL', 'IMPUTED'));

ALTER TABLE public.trip_budget_lines DROP CONSTRAINT IF EXISTS trip_budget_lines_source_type_check;
ALTER TABLE public.trip_budget_lines ADD CONSTRAINT trip_budget_lines_source_type_check
    CHECK (source_type IN ('OFFICIAL_API', 'SCRAPE', 'ESTIMATE', 'MANUAL', 'IMPUTED'));
