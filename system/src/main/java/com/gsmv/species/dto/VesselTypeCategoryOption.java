package com.gsmv.species.dto;

public record VesselTypeCategoryOption(
        Long id,
        Long parentId,
        String rank,
        String scientificName,
        String chineseName
) {
}
