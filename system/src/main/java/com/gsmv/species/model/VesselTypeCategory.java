package com.gsmv.species.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VesselTypeCategory {

    private Long id;
    private Long parentId;
    private String rank;
    private String scientificName;
    private String chineseName;
    private LocalDateTime createdAt;
}
