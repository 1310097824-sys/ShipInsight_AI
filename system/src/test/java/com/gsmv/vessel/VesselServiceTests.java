package com.gsmv.vessel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gsmv.ais.AisService;
import com.gsmv.ai.rag.RagKnowledgeService;
import com.gsmv.ais.dto.AisVesselDraftBatchResult;
import com.gsmv.ais.dto.AisVesselDraftCandidate;
import com.gsmv.audit.service.AuditService;
import com.gsmv.media.MediaFileService;
import com.gsmv.versioning.EntityVersionService;
import com.gsmv.vessel.dto.VesselDetailView;
import com.gsmv.vessel.dto.VesselSaveRequest;
import com.gsmv.vessel.mapper.VesselMapper;
import com.gsmv.vessel.mapper.VesselTypeMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class VesselServiceTests {

    private final VesselMapper vesselMapper = mock(VesselMapper.class);
    private final VesselTypeMapper vesselTypeMapper = mock(VesselTypeMapper.class);
    private final MediaFileService mediaFileService = mock(MediaFileService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final EntityVersionService entityVersionService = mock(EntityVersionService.class);
    private final AisService aisService = mock(AisService.class);
    private final RagKnowledgeService ragKnowledgeService = mock(RagKnowledgeService.class);
    private final PlatformTransactionManager transactionManager = new StubTransactionManager();

    private final TestVesselService vesselService = new TestVesselService(
            vesselMapper,
            vesselTypeMapper,
            mediaFileService,
            auditService,
            entityVersionService,
            aisService,
            ragKnowledgeService,
            transactionManager
    );

    @Test
    void generateVesselDraftsFromAisScansAllBatchesWhenLimitIsUnset() {
        List<AisVesselDraftCandidate> firstBatch = IntStream.range(0, 1000)
                .mapToObj(index -> candidate("rec-" + index, "111000" + index, null))
                .toList();
        AisVesselDraftCandidate lastCandidate = candidate("rec-last", "222000222", "IMO222");

        when(aisService.vesselDraftCandidates(null, null, null, 1000, 0))
                .thenReturn(firstBatch);
        when(aisService.vesselDraftCandidates(null, null, null, 1000, 1000))
                .thenReturn(List.of(lastCandidate));
        when(vesselMapper.findByMmsi(any(), eq(null))).thenReturn(null);
        when(vesselMapper.findByImo(any(), eq(null))).thenReturn(null);

        AisVesselDraftBatchResult result = vesselService.generateVesselDraftsFromAis(null);

        assertThat(result.scanned()).isEqualTo(1001);
        assertThat(result.created()).isEqualTo(1001);
        assertThat(result.skippedExisting()).isZero();
        assertThat(result.skippedInvalid()).isZero();
        assertThat(result.limit()).isZero();

        verify(aisService).vesselDraftCandidates(null, null, null, 1000, 0);
        verify(aisService).vesselDraftCandidates(null, null, null, 1000, 1000);
        assertThat(vesselService.createdRequests).hasSize(1001);
        verify(vesselMapper, never()).insert(any());
    }

    @Test
    void generateVesselDraftsFromAisHonorsExplicitLimitWithinSingleBatch() {
        AisVesselDraftCandidate first = candidate("rec-1", "111000111", null);
        AisVesselDraftCandidate second = candidate("rec-2", "222000222", null);
        AisVesselDraftCandidate third = candidate("rec-3", "333000333", null);

        when(aisService.vesselDraftCandidates(null, null, null, 3, 0))
                .thenReturn(List.of(first, second, third));
        when(vesselMapper.findByMmsi(eq("111000111"), eq(null))).thenReturn(null);
        when(vesselMapper.findByMmsi(eq("222000222"), eq(null))).thenReturn(null);
        when(vesselMapper.findByMmsi(eq("333000333"), eq(null))).thenReturn(null);

        AisVesselDraftBatchResult result = vesselService.generateVesselDraftsFromAis(
                new com.gsmv.ais.dto.AisVesselDraftBatchRequest(null, null, null, 3)
        );

        assertThat(result.scanned()).isEqualTo(3);
        assertThat(result.created()).isEqualTo(3);
        assertThat(result.skippedExisting()).isZero();
        assertThat(result.skippedInvalid()).isZero();
        assertThat(result.limit()).isEqualTo(3);

        verify(aisService).vesselDraftCandidates(null, null, null, 3, 0);
        assertThat(vesselService.createdRequests).hasSize(3);
        verify(vesselMapper, never()).insert(any());
    }

    private static AisVesselDraftCandidate candidate(String recordId, String mmsi, String imo) {
        return new AisVesselDraftCandidate(
                recordId,
                mmsi,
                imo,
                "Test Vessel",
                "CALL",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(5),
                "ais.csv",
                LocalDateTime.of(2025, 3, 1, 0, 0, 1)
        );
    }

    private static final class StubTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }

    private static final class TestVesselService extends VesselService {
        private final List<VesselSaveRequest> createdRequests = new java.util.ArrayList<>();

        private TestVesselService(
                VesselMapper vesselMapper,
                VesselTypeMapper vesselTypeMapper,
                MediaFileService mediaFileService,
                AuditService auditService,
                EntityVersionService entityVersionService,
                AisService aisService,
                RagKnowledgeService ragKnowledgeService,
                PlatformTransactionManager transactionManager
        ) {
            super(vesselMapper, vesselTypeMapper, mediaFileService, auditService, entityVersionService, aisService, ragKnowledgeService, transactionManager);
        }

        @Override
        public VesselDetailView createVessel(VesselSaveRequest request) {
            createdRequests.add(request);
            return new VesselDetailView(
                    1L,
                    request.vesselName(),
                    request.mmsi(),
                    request.imo(),
                    request.callSign(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    request.lengthM(),
                    request.widthM(),
                    request.draftM(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    request.note(),
                    request.sourceText(),
                    request.status(),
                    null,
                    null,
                    List.of()
            );
        }
    }
}
