package com.gsmv.ecosystem.mapper;

import com.gsmv.ecosystem.model.ShippingZone;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShippingZoneMapper {

    @Select("""
            <script>
            SELECT *
            FROM shipping_zone
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (
                  name LIKE CONCAT('%', #{keyword}, '%')
                  OR type LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test='type != null and type != ""'>
                AND type = #{type}
              </if>
            </where>
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ShippingZone> findPage(
            @Param("keyword") String keyword,
            @Param("type") String type,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM shipping_zone
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (
                  name LIKE CONCAT('%', #{keyword}, '%')
                  OR type LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test='type != null and type != ""'>
                AND type = #{type}
              </if>
            </where>
            </script>
            """)
    long count(@Param("keyword") String keyword, @Param("type") String type);

    @Select("SELECT * FROM shipping_zone WHERE id = #{id}")
    ShippingZone findById(Long id);

    @Select("SELECT * FROM shipping_zone ORDER BY name")
    List<ShippingZone> findAll();

    @Insert("""
            INSERT INTO shipping_zone (name, type, description)
            VALUES (#{name}, #{type}, #{description})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ShippingZone ecosystem);

    @Update("""
            UPDATE shipping_zone
            SET name = #{name},
                type = #{type},
                description = #{description}
            WHERE id = #{id}
            """)
    void update(ShippingZone ecosystem);

    @Delete("DELETE FROM shipping_zone WHERE id = #{id}")
    void deleteById(Long id);

    @Select("SELECT COUNT(*) FROM observation WHERE ecosystem_id = #{id}")
    long countObservationReferences(Long id);
}
