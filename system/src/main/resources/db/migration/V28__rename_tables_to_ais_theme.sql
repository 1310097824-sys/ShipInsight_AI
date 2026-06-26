-- V28: Rename legacy marine-biology tables to AIS vessel-traffic theme
-- Drops empty species/taxon tables (replaced by vessel_profile/vessel_type from V17)
-- Renames ecosystem → shipping_zone, observation → ais_record_manual

SET FOREIGN_KEY_CHECKS = 0;

-- Old species/taxon tables are empty (cleaned in V17) and replaced by vessel_profile/vessel_type
DROP TABLE IF EXISTS species;
DROP TABLE IF EXISTS taxon;

-- Rename remaining tables to AIS theme
RENAME TABLE ecosystem TO shipping_zone;
RENAME TABLE observation TO ais_record_manual;
RENAME TABLE observation_species TO ais_record_manual_vessel;

-- Rename the FK column on the junction table after species table is gone
ALTER TABLE ais_record_manual_vessel CHANGE COLUMN species_id vessel_id BIGINT NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
