package com.gsmv.vessel.mapper;

import com.gsmv.vessel.dto.VesselRow;
import com.gsmv.vessel.model.VesselProfile;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VesselMapper {

    @Select("""
            <script>
            SELECT
              v.id,
              v.vessel_name,
              v.mmsi,
              v.imo,
              v.call_sign,
              v.vessel_type_id,
              vt.name AS vessel_type_name,
              vt.code AS vessel_type_code,
              vt.parent_id AS vessel_type_parent_id,
              v.flag_state,
              v.operator_name,
              v.owner_name,
              v.length_m,
              v.width_m,
              v.draft_m,
              v.gross_tonnage,
              v.deadweight_tonnage,
              v.risk_level,
              v.navigation_status,
              v.home_port,
              v.usual_region,
              v.route_area,
              v.note,
              v.source_text,
              v.status,
              v.created_at,
              v.updated_at
            FROM vessel_profile v
            LEFT JOIN vessel_type vt ON vt.id = v.vessel_type_id
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (
                  v.vessel_name LIKE CONCAT('%', #{keyword}, '%')
                  OR v.mmsi LIKE CONCAT('%', #{keyword}, '%')
                  OR v.imo LIKE CONCAT('%', #{keyword}, '%')
                  OR v.call_sign LIKE CONCAT('%', #{keyword}, '%')
                  OR v.operator_name LIKE CONCAT('%', #{keyword}, '%')
                  OR v.owner_name LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test='status != null'>
                AND v.status = #{status}
              </if>
              <if test='riskLevel != null and riskLevel != ""'>
                AND v.risk_level = #{riskLevel}
              </if>
              <if test='navigationStatus != null and navigationStatus != ""'>
                AND v.navigation_status = #{navigationStatus}
              </if>
              <if test='routeKeyword != null and routeKeyword != ""'>
                AND (
                  v.usual_region LIKE CONCAT('%', #{routeKeyword}, '%')
                  OR v.route_area LIKE CONCAT('%', #{routeKeyword}, '%')
                  OR v.home_port LIKE CONCAT('%', #{routeKeyword}, '%')
                )
              </if>
              <if test='typeIds != null and typeIds.size() > 0'>
                AND v.vessel_type_id IN
                <foreach collection='typeIds' item='typeId' open='(' separator=',' close=')'>
                  #{typeId}
                </foreach>
              </if>
            </where>
            ORDER BY v.updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<VesselRow> findPage(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("typeIds") List<Long> typeIds,
            @Param("riskLevel") String riskLevel,
            @Param("navigationStatus") String navigationStatus,
            @Param("routeKeyword") String routeKeyword,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM vessel_profile v
            LEFT JOIN vessel_type vt ON vt.id = v.vessel_type_id
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (
                  v.vessel_name LIKE CONCAT('%', #{keyword}, '%')
                  OR v.mmsi LIKE CONCAT('%', #{keyword}, '%')
                  OR v.imo LIKE CONCAT('%', #{keyword}, '%')
                  OR v.call_sign LIKE CONCAT('%', #{keyword}, '%')
                  OR v.operator_name LIKE CONCAT('%', #{keyword}, '%')
                  OR v.owner_name LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test='status != null'>
                AND v.status = #{status}
              </if>
              <if test='riskLevel != null and riskLevel != ""'>
                AND v.risk_level = #{riskLevel}
              </if>
              <if test='navigationStatus != null and navigationStatus != ""'>
                AND v.navigation_status = #{navigationStatus}
              </if>
              <if test='routeKeyword != null and routeKeyword != ""'>
                AND (
                  v.usual_region LIKE CONCAT('%', #{routeKeyword}, '%')
                  OR v.route_area LIKE CONCAT('%', #{routeKeyword}, '%')
                  OR v.home_port LIKE CONCAT('%', #{routeKeyword}, '%')
                )
              </if>
              <if test='typeIds != null and typeIds.size() > 0'>
                AND v.vessel_type_id IN
                <foreach collection='typeIds' item='typeId' open='(' separator=',' close=')'>
                  #{typeId}
                </foreach>
              </if>
            </where>
            </script>
            """)
    long count(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("typeIds") List<Long> typeIds,
            @Param("riskLevel") String riskLevel,
            @Param("navigationStatus") String navigationStatus,
            @Param("routeKeyword") String routeKeyword
    );

    @Select("""
            SELECT
              v.id,
              v.vessel_name,
              v.mmsi,
              v.imo,
              v.call_sign,
              v.vessel_type_id,
              vt.name AS vessel_type_name,
              vt.code AS vessel_type_code,
              vt.parent_id AS vessel_type_parent_id,
              v.flag_state,
              v.operator_name,
              v.owner_name,
              v.length_m,
              v.width_m,
              v.draft_m,
              v.gross_tonnage,
              v.deadweight_tonnage,
              v.risk_level,
              v.navigation_status,
              v.home_port,
              v.usual_region,
              v.route_area,
              v.note,
              v.source_text,
              v.status,
              v.created_at,
              v.updated_at
            FROM vessel_profile v
            LEFT JOIN vessel_type vt ON vt.id = v.vessel_type_id
            WHERE v.id = #{id}
            """)
    VesselRow findRowById(Long id);

    @Select("SELECT * FROM vessel_profile WHERE id = #{id}")
    VesselProfile findById(Long id);

    @Select("""
            <script>
            SELECT * FROM vessel_profile
            WHERE mmsi = #{mmsi}
            <if test='excludeId != null'>
              AND id != #{excludeId}
            </if>
            LIMIT 1
            </script>
            """)
    VesselProfile findByMmsi(@Param("mmsi") String mmsi, @Param("excludeId") Long excludeId);

    @Select("""
            <script>
            SELECT * FROM vessel_profile
            WHERE imo = #{imo}
            <if test='excludeId != null'>
              AND id != #{excludeId}
            </if>
            LIMIT 1
            </script>
            """)
    VesselProfile findByImo(@Param("imo") String imo, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO vessel_profile (
              vessel_name, mmsi, imo, call_sign, vessel_type_id, flag_state,
              operator_name, owner_name, length_m, width_m, draft_m,
              gross_tonnage, deadweight_tonnage, risk_level, navigation_status,
              home_port, usual_region, route_area, note, source_text, status
            ) VALUES (
              #{vesselName}, #{mmsi}, #{imo}, #{callSign}, #{vesselTypeId}, #{flagState},
              #{operatorName}, #{ownerName}, #{lengthM}, #{widthM}, #{draftM},
              #{grossTonnage}, #{deadweightTonnage}, #{riskLevel}, #{navigationStatus},
              #{homePort}, #{usualRegion}, #{routeArea}, #{note}, #{sourceText}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(VesselProfile vessel);

    @Update("""
            UPDATE vessel_profile
            SET vessel_name = #{vesselName},
                mmsi = #{mmsi},
                imo = #{imo},
                call_sign = #{callSign},
                vessel_type_id = #{vesselTypeId},
                flag_state = #{flagState},
                operator_name = #{operatorName},
                owner_name = #{ownerName},
                length_m = #{lengthM},
                width_m = #{widthM},
                draft_m = #{draftM},
                gross_tonnage = #{grossTonnage},
                deadweight_tonnage = #{deadweightTonnage},
                risk_level = #{riskLevel},
                navigation_status = #{navigationStatus},
                home_port = #{homePort},
                usual_region = #{usualRegion},
                route_area = #{routeArea},
                note = #{note},
                source_text = #{sourceText},
                status = #{status},
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{id}
            """)
    void update(VesselProfile vessel);

    @Update("UPDATE vessel_profile SET status = 0, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id}")
    void archiveById(Long id);
}
