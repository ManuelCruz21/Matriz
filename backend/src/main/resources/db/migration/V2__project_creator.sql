-- Creator identity was not stored in V1. Preserve unknown legacy creators as NULL;
-- an explicit, evidence-based backfill is required before full OpenAPI conformance.
ALTER TABLE projects
    ADD COLUMN created_by_user_id BIGINT REFERENCES app_users(id);
