ALTER TABLE ai_research_report
    ADD COLUMN period_start DATETIME(3) NULL AFTER days,
    ADD COLUMN period_end DATETIME(3) NULL AFTER period_start,
    ADD COLUMN metrics_json JSON NULL AFTER evidence_json;
