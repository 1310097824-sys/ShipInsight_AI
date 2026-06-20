package com.gsmv.vessel.mapper;

import com.gsmv.vessel.model.VesselType;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VesselTypeMapper {

    @Select("SELECT id, parent_id, code, name, description, created_at FROM vessel_type ORDER BY parent_id, name")
    List<VesselType> findAll();

    @Select("SELECT id, parent_id, code, name, description, created_at FROM vessel_type WHERE id = #{id}")
    VesselType findById(Long id);
}
