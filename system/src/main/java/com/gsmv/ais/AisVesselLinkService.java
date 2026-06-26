package com.gsmv.ais;

import com.gsmv.ais.dto.AisLinkedVesselView;
import com.gsmv.ais.dto.AisRecordView;
import com.gsmv.vessel.mapper.VesselMapper;
import com.gsmv.vessel.model.VesselProfile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AisVesselLinkService {

    private final VesselMapper vesselMapper;

    public AisVesselLinkService(VesselMapper vesselMapper) {
        this.vesselMapper = vesselMapper;
    }

    public List<AisRecordView> enrich(List<AisRecordView> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<String, AisLinkedVesselView> mmsiCache = new HashMap<>();
        Map<String, AisLinkedVesselView> imoCache = new HashMap<>();
        return records.stream()
                .map(record -> record.withLinkedVessel(link(record.mmsi(), record.imo(), mmsiCache, imoCache)))
                .toList();
    }

    public AisLinkedVesselView link(String mmsi, String imo) {
        return link(mmsi, imo, new HashMap<>(), new HashMap<>());
    }

    private AisLinkedVesselView link(
            String mmsi,
            String imo,
            Map<String, AisLinkedVesselView> mmsiCache,
            Map<String, AisLinkedVesselView> imoCache
    ) {
        String normalizedMmsi = normalizeIdentity(mmsi);
        if (normalizedMmsi != null) {
            if (!mmsiCache.containsKey(normalizedMmsi)) {
                mmsiCache.put(normalizedMmsi, toLinkedVessel(vesselMapper.findByMmsi(normalizedMmsi, null), "MMSI"));
            }
            AisLinkedVesselView linkedByMmsi = mmsiCache.get(normalizedMmsi);
            if (linkedByMmsi != null) {
                return linkedByMmsi;
            }
        }

        String normalizedImo = normalizeIdentity(imo);
        if (normalizedImo != null) {
            if (!imoCache.containsKey(normalizedImo)) {
                imoCache.put(normalizedImo, toLinkedVessel(vesselMapper.findByImo(normalizedImo, null), "IMO"));
            }
            return imoCache.get(normalizedImo);
        }
        return null;
    }

    private AisLinkedVesselView toLinkedVessel(VesselProfile vessel, String matchMethod) {
        if (vessel == null) {
            return null;
        }
        return new AisLinkedVesselView(
                vessel.getId(),
                vessel.getVesselName(),
                vessel.getMmsi(),
                vessel.getImo(),
                vessel.getRiskLevel(),
                vessel.getNavigationStatus(),
                vessel.getStatus(),
                matchMethod
        );
    }

    private String normalizeIdentity(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
