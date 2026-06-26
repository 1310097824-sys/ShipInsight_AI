package com.gsmv.observation.mapper;

import com.gsmv.observation.dto.AisRecordManualVesselInput;
import com.gsmv.observation.dto.AisRecordManualVesselView;
import com.gsmv.observation.dto.AisRecordManualView;
import com.gsmv.observation.model.AisRecordManual;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AisRecordManualMapper {

    @Select("""
            <script>
            SELECT
              o.id,
              o.ecosystem_id,
              e.name AS ecosystem_name,
              o.observer_user_id,
              u.display_name AS observer_name,
              o.observed_at,
              o.location_lat,
              o.location_lng,
              o.location_name,
              o.env_json,
              o.note,
              o.created_at
            FROM ais_record_manual o
            JOIN shipping_zone e ON e.id = o.ecosystem_id
            JOIN sys_user u ON u.id = o.observer_user_id
            <where>
              <if test='ecosystemId != null'>
                AND o.ecosystem_id = #{ecosystemId}
              </if>
              <if test='keyword != null and keyword != ""'>
                AND (
                  e.name LIKE CONCAT('%', #{keyword}, '%')
                  OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                  OR o.location_name LIKE CONCAT('%', #{keyword}, '%')
                  OR o.note LIKE CONCAT('%', #{keyword}, '%')
                  OR EXISTS (
                    SELECT 1
                    FROM ais_record_manual_vessel osv
                    JOIN vessel_profile vp ON vp.id = osv.vessel_id
                    WHERE osv.observation_id = o.id
                      AND vp.vessel_name LIKE CONCAT('%', #{keyword}, '%')
                  )
                )
              </if>
              <if test='observedFrom != null'>
                AND o.observed_at &gt;= #{observedFrom}
              </if>
              <if test='observedTo != null'>
                AND o.observed_at &lt;= #{observedTo}
              </if>
            </where>
            ORDER BY o.observed_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AisRecordManualView> findPage(
            @Param("ecosystemId") Long ecosystemId,
            @Param("keyword") String keyword,
            @Param("observedFrom") LocalDateTime observedFrom,
            @Param("observedTo") LocalDateTime observedTo,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM ais_record_manual o
            JOIN shipping_zone e ON e.id = o.ecosystem_id
            JOIN sys_user u ON u.id = o.observer_user_id
            <where>
              <if test='ecosystemId != null'>
                AND o.ecosystem_id = #{ecosystemId}
              </if>
              <if test='keyword != null and keyword != ""'>
                AND (
                  e.name LIKE CONCAT('%', #{keyword}, '%')
                  OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                  OR o.location_name LIKE CONCAT('%', #{keyword}, '%')
                  OR o.note LIKE CONCAT('%', #{keyword}, '%')
                  OR EXISTS (
                    SELECT 1
                    FROM ais_record_manual_vessel osv
                    JOIN vessel_profile vp ON vp.id = osv.vessel_id
                    WHERE osv.observation_id = o.id
                      AND vp.vessel_name LIKE CONCAT('%', #{keyword}, '%')
                  )
                )
              </if>
              <if test='observedFrom != null'>
                AND o.observed_at &gt;= #{observedFrom}
              </if>
              <if test='observedTo != null'>
                AND o.observed_at &lt;= #{observedTo}
              </if>
            </where>
            </script>
            """)
    long count(
            @Param("ecosystemId") Long ecosystemId,
            @Param("keyword") String keyword,
            @Param("observedFrom") LocalDateTime observedFrom,
            @Param("observedTo") LocalDateTime observedTo
    );

    @Select("""
            SELECT
              o.id,
              o.ecosystem_id,
              e.name AS ecosystem_name,
              o.observer_user_id,
              u.display_name AS observer_name,
              o.observed_at,
              o.location_lat,
              o.location_lng,
              o.location_name,
              o.env_json,
              o.note,
              o.created_at
            FROM ais_record_manual o
            JOIN shipping_zone e ON e.id = o.ecosystem_id
            JOIN sys_user u ON u.id = o.observer_user_id
            WHERE o.id = #{id}
            """)
    AisRecordManualView findViewById(Long id);

    @Select("SELECT * FROM ais_record_manual WHERE id = #{id}")
    AisRecordManual findById(Long id);

    @Insert("""
            INSERT INTO ais_record_manual (
              ecosystem_id, observer_user_id, observed_at, location_lat, location_lng,
              location_point, location_name, env_json, note
            ) VALUES (
              #{ecosystemId}, #{observerUserId}, #{observedAt}, #{locationLat}, #{locationLng},
              ST_SRID(Point(#{locationLng}, #{locationLat}), 4326), #{locationName}, #{envJson}, #{note}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AisRecordManual observation);

    @Insert("""
            INSERT INTO ais_record_manual (
              id, ecosystem_id, observer_user_id, observed_at, location_lat, location_lng,
              location_point, location_name, env_json, note
            ) VALUES (
              #{id}, #{ecosystemId}, #{observerUserId}, #{observedAt}, #{locationLat}, #{locationLng},
              ST_SRID(Point(#{locationLng}, #{locationLat}), 4326), #{locationName}, #{envJson}, #{note}
            )
            """)
    void insertWithId(AisRecordManual observation);

    @Update("""
            UPDATE ais_record_manual
            SET ecosystem_id = #{ecosystemId},
                observer_user_id = #{observerUserId},
                observed_at = #{observedAt},
                location_lat = #{locationLat},
                location_lng = #{locationLng},
                location_point = ST_SRID(Point(#{locationLng}, #{locationLat}), 4326),
                location_name = #{locationName},
                env_json = #{envJson},
                note = #{note}
            WHERE id = #{id}
            """)
    void update(AisRecordManual observation);

    @Insert("""
            <script>
            INSERT INTO ais_record_manual_vessel (observation_id, vessel_id, count_estimated, behavior, comment)
            VALUES
            <foreach collection='items' item='item' separator=','>
              (#{observationId}, #{item.vesselId}, #{item.countEstimated}, #{item.behavior}, #{item.comment})
            </foreach>
            </script>
            """)
    void insertVesselBatch(@Param("observationId") Long observationId, @Param("items") List<AisRecordManualVesselInput> items);

    @Delete("DELETE FROM ais_record_manual_vessel WHERE observation_id = #{observationId}")
    void deleteVesselsByObservationId(Long observationId);

    @Delete("DELETE FROM ais_record_manual WHERE id = #{id}")
    void deleteById(Long id);

    @Select("""
            SELECT
              os.vessel_id,
              v.vessel_name AS display_name,
              v.vessel_name AS profile_name,
              v.status,
              os.count_estimated,
              os.behavior,
              os.comment
            FROM ais_record_manual_vessel os
            JOIN vessel_profile v ON v.id = os.vessel_id
            WHERE os.observation_id = #{observationId}
            ORDER BY os.id
            """)
    List<AisRecordManualVesselView> findVesselViews(Long observationId);
}
