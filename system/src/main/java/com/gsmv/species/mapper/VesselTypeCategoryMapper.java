package com.gsmv.species.mapper;

import com.gsmv.species.model.VesselTypeCategory;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VesselTypeCategoryMapper {

    @Select("SELECT id, parent_id, code, name, description, created_at FROM vessel_type ORDER BY parent_id, code")
    List<VesselTypeCategory> findAll();

    @Select("SELECT id, parent_id, code, name, description, created_at FROM vessel_type WHERE id = #{id}")
    VesselTypeCategory findById(Long id);

    @Select("""
            <script>
            SELECT id, parent_id, code, name, description, created_at
            FROM vessel_type
            WHERE code = #{code}
              <choose>
                <when test='parentId != null'>
                  AND parent_id = #{parentId}
                </when>
                <otherwise>
                  AND parent_id IS NULL
                </otherwise>
              </choose>
            LIMIT 1
            </script>
            """)
    VesselTypeCategory findByParentAndCode(
            @Param("parentId") Long parentId,
            @Param("code") String code
    );

    @Insert("""
            INSERT INTO vessel_type (parent_id, code, name, description)
            VALUES (#{parentId}, #{scientificName}, #{chineseName}, #{description})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(VesselTypeCategory vesselType);

    @Update("""
            UPDATE vessel_type
            SET name = #{name}
            WHERE id = #{id}
            """)
    void updateName(@Param("id") Long id, @Param("name") String name);
}
