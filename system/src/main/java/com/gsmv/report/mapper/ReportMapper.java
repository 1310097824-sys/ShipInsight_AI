package com.gsmv.report.mapper;

import com.gsmv.report.dto.EcosystemAnalyticsPoint;
import com.gsmv.report.dto.NameValuePoint;
import com.gsmv.report.dto.AisRecordMapPoint;
import com.gsmv.report.dto.VesselDistributionPoint;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {

    @Select("SELECT COUNT(*) FROM vessel_profile WHERE status = 1")
    long countActiveSpecies();

    @Select("SELECT COUNT(*) FROM ais_record_manual")
    long countObservations();

    @Select("SELECT COUNT(*) FROM shipping_zone")
    long countEcosystems();

    @Select("SELECT COUNT(*) FROM sys_user WHERE status = 1")
    long countActiveUsers();

    @Select("SELECT COUNT(*) FROM ais_record_manual WHERE observed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    long countRecentObservations();

    @Select("""
            SELECT COALESCE(NULLIF(risk_level, ''), '未分级') AS name, COUNT(*) AS value
            FROM vessel_profile
            WHERE status = 1
            GROUP BY COALESCE(NULLIF(risk_level, ''), '未分级')
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> riskDistribution();

    @Select("""
            SELECT COALESCE(NULLIF(navigation_status, ''), '未知') AS name, COUNT(*) AS value
            FROM vessel_profile
            WHERE status = 1
            GROUP BY COALESCE(NULLIF(navigation_status, ''), '未知')
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> iucnStatusDistribution();

    @Select("""
            SELECT COALESCE(NULLIF(risk_level, ''), '未分级') AS name, COUNT(*) AS value
            FROM vessel_profile
            WHERE status = 1
            GROUP BY COALESCE(NULLIF(risk_level, ''), '未分级')
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> protectionLevelDistribution();

    @Select("""
            SELECT COALESCE(NULLIF(vt.name, ''), '未分类') AS name, COUNT(*) AS value
            FROM vessel_profile vp
            LEFT JOIN vessel_type vt ON vt.id = vp.vessel_type_id
            WHERE vp.status = 1 AND vt.parent_id IS NULL
            GROUP BY vt.id, COALESCE(NULLIF(vt.name, ''), '未分类')
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> speciesPhylumDistribution();

    @Select("""
            SELECT COALESCE(NULLIF(child.name, ''), '未分类') AS name, COUNT(*) AS value
            FROM vessel_profile vp
            LEFT JOIN vessel_type child ON child.id = vp.vessel_type_id
            WHERE vp.status = 1
            GROUP BY child.id, COALESCE(NULLIF(child.name, ''), '未分类')
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> speciesClassDistribution();

    @Select("""
            SELECT DATE_FORMAT(observed_at, '%Y-%m-%d') AS name, COUNT(*) AS value
            FROM ais_record_manual
            WHERE observed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE_FORMAT(observed_at, '%Y-%m-%d')
            ORDER BY name
            """)
    List<NameValuePoint> observationTrend(@Param("days") int days);

    @Select("""
            SELECT COALESCE(NULLIF(u.display_name, ''), u.username) AS name, COUNT(a.id) AS value
            FROM ais_record_manual a
            JOIN sys_user u ON u.id = a.observer_user_id
            WHERE a.observed_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY u.id, COALESCE(NULLIF(u.display_name, ''), u.username)
            ORDER BY value DESC, name ASC
            """)
    List<NameValuePoint> observationActivityByUser(@Param("days") int days);

    @Select("""
            SELECT
                e.id AS zone_id,
                e.name AS zone_name,
                e.type AS zone_type,
                COUNT(DISTINCT a.id) AS record_count,
                COUNT(DISTINCT av.vessel_id) AS linked_vessel_count
            FROM shipping_zone e
            LEFT JOIN ais_record_manual a ON a.ecosystem_id = e.id
            LEFT JOIN ais_record_manual_vessel av ON av.observation_id = a.id
            GROUP BY e.id, e.name, e.type
            ORDER BY record_count DESC, linked_vessel_count DESC, e.name ASC
            """)
    List<EcosystemAnalyticsPoint> ecosystemAnalytics();

    @Select("""
            SELECT
                vp.id AS vessel_id,
                vp.vessel_name AS vessel_name,
                vp.vessel_name AS display_name,
                vp.vessel_name AS profile_name,
                vp.length_m AS location_lat,
                vp.width_m AS location_lng,
                vp.route_area AS route_description,
                vp.risk_level AS risk_level,
                vp.navigation_status AS operational_status
            FROM vessel_profile vp
            WHERE vp.status = 1
              AND vp.length_m IS NOT NULL
              AND vp.width_m IS NOT NULL
            ORDER BY vp.vessel_name ASC
            """)
    List<VesselDistributionPoint> speciesDistributionPoints();

    @Select("""
            SELECT
                a.id AS record_id,
                e.name AS shipping_zone_name,
                COALESCE(NULLIF(u.display_name, ''), u.username) AS recorder_name,
                a.observed_at,
                a.location_lat,
                a.location_lng,
                a.location_name,
                COUNT(DISTINCT av.vessel_id) AS linked_vessel_count,
                a.note
            FROM ais_record_manual a
            JOIN shipping_zone e ON e.id = a.ecosystem_id
            JOIN sys_user u ON u.id = a.observer_user_id
            LEFT JOIN ais_record_manual_vessel av ON av.observation_id = a.id
            GROUP BY a.id, e.name, COALESCE(NULLIF(u.display_name, ''), u.username), a.observed_at,
                     a.location_lat, a.location_lng, a.location_name, a.note
            ORDER BY a.observed_at DESC
            """)
    List<AisRecordMapPoint> observationMapPoints();
}
