package com.gsmv.ais;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gsmv.ais.dto.AisLinkedVesselView;
import com.gsmv.vessel.mapper.VesselMapper;
import com.gsmv.vessel.model.VesselProfile;
import org.junit.jupiter.api.Test;

class AisVesselLinkServiceTests {

    private final VesselMapper vesselMapper = mock(VesselMapper.class);
    private final AisVesselLinkService linkService = new AisVesselLinkService(vesselMapper);

    @Test
    void linksByMmsiFirst() {
        VesselProfile vessel = vessel(1L, "MMSI SHIP", "111000111", "IMO111");
        when(vesselMapper.findByMmsi(eq("111000111"), isNull())).thenReturn(vessel);

        AisLinkedVesselView linked = linkService.link("111000111", "IMO999");

        assertThat(linked).isNotNull();
        assertThat(linked.vesselId()).isEqualTo(1L);
        assertThat(linked.matchMethod()).isEqualTo("MMSI");
    }

    @Test
    void fallsBackToImoWhenMmsiDoesNotMatch() {
        VesselProfile vessel = vessel(2L, "IMO SHIP", null, "IMO222");
        when(vesselMapper.findByMmsi(eq("222000222"), isNull())).thenReturn(null);
        when(vesselMapper.findByImo(eq("IMO222"), isNull())).thenReturn(vessel);

        AisLinkedVesselView linked = linkService.link("222000222", "IMO222");

        assertThat(linked).isNotNull();
        assertThat(linked.vesselId()).isEqualTo(2L);
        assertThat(linked.matchMethod()).isEqualTo("IMO");
    }

    @Test
    void mmsiMatchWinsWhenImoPointsToAnotherVessel() {
        VesselProfile mmsiVessel = vessel(3L, "MMSI WINNER", "333000333", "IMO333");
        VesselProfile imoVessel = vessel(4L, "IMO LOSER", null, "IMO444");
        when(vesselMapper.findByMmsi(eq("333000333"), isNull())).thenReturn(mmsiVessel);
        when(vesselMapper.findByImo(eq("IMO444"), isNull())).thenReturn(imoVessel);

        AisLinkedVesselView linked = linkService.link("333000333", "IMO444");

        assertThat(linked).isNotNull();
        assertThat(linked.vesselId()).isEqualTo(3L);
        assertThat(linked.matchMethod()).isEqualTo("MMSI");
    }

    @Test
    void returnsNullWhenNoIdentityMatches() {
        when(vesselMapper.findByMmsi(eq("555000555"), isNull())).thenReturn(null);
        when(vesselMapper.findByImo(eq("IMO555"), isNull())).thenReturn(null);

        assertThat(linkService.link("555000555", "IMO555")).isNull();
    }

    private VesselProfile vessel(Long id, String name, String mmsi, String imo) {
        VesselProfile vessel = new VesselProfile();
        vessel.setId(id);
        vessel.setVesselName(name);
        vessel.setMmsi(mmsi);
        vessel.setImo(imo);
        vessel.setRiskLevel("普通关注");
        vessel.setNavigationStatus("在航");
        vessel.setStatus(1);
        return vessel;
    }
}
