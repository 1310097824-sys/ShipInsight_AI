export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface AuthProfile {
  id: number
  username: string
  displayName: string
  avatarUrl: string
  roles: string[]
  authorities: string[]
}

export interface LoginResult {
  accessToken: string
  expiresInSeconds: number
  user: AuthProfile
}

export interface RoleOption {
  id: number
  code: string
  name: string
  description: string
}

export interface UserView {
  id: number
  username: string
  displayName: string
  email?: string
  phone?: string
  bio?: string
  avatarUrl: string
  status: number
  approvalStatus: string
  approvalRemark?: string
  reviewedAt?: string
  lastLoginAt?: string
  createdAt: string
  roles: RoleOption[]
}

export interface UserProfileView {
  id: number
  username: string
  displayName: string
  email?: string
  phone?: string
  bio?: string
  avatarUrl: string
  status: number
  approvalStatus: string
  lastLoginAt?: string
  createdAt: string
  roles: RoleOption[]
}

export interface VesselTypeCategoryOption {
  id: number
  parentId?: number
  level: string
  code: string
  name?: string
}

export interface VesselProfileView {
  id: number
  typeCategoryId: number
  rank: string
  profileName: string
  displayName?: string
  typePath?: string
  riskLevel?: string
  operationalStatus?: string
  routeDescription?: string
  mmsi?: string
  imo?: string
  vesselTypeId?: number
  status: number
  createdAt: string
  updatedAt: string
}

export interface VesselProfileImageView {
  id: number
  url: string
  originalFilename: string
}

export interface VesselProfileDetailView extends VesselProfileView {
  description?: string
  specifications?: string
  operationalPattern?: string
  usualRegion?: string
  routeCoverage?: string
  homeLat?: number
  homeLng?: number
  videoUrl?: string
  referenceText?: string
  images: VesselProfileImageView[]
}

export interface VesselTypeOption {
  id: number
  parentId?: number | null
  code: string
  name: string
  description?: string
}

export interface VesselImageView {
  id: number
  url: string
  originalFilename: string
}

export interface VesselView {
  id: number
  vesselName: string
  mmsi?: string
  imo?: string
  callSign?: string
  vesselTypeId?: number | null
  vesselTypeName?: string
  vesselTypePath?: string
  flagState?: string
  operatorName?: string
  lengthM?: number | null
  widthM?: number | null
  draftM?: number | null
  riskLevel?: string
  navigationStatus?: string
  usualRegion?: string
  routeArea?: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface VesselDetailView extends VesselView {
  ownerName?: string
  grossTonnage?: number | null
  deadweightTonnage?: number | null
  homePort?: string
  note?: string
  sourceText?: string
  images: VesselImageView[]
}

export interface VesselSavePayload {
  vesselName: string
  mmsi?: string
  imo?: string
  callSign?: string
  vesselTypeId?: number | null
  flagState?: string
  operatorName?: string
  ownerName?: string
  lengthM?: number | null
  widthM?: number | null
  draftM?: number | null
  grossTonnage?: number | null
  deadweightTonnage?: number | null
  riskLevel?: string
  navigationStatus?: string
  homePort?: string
  usualRegion?: string
  routeArea?: string
  note?: string
  sourceText?: string
  status: number
}

export interface AisLinkedVesselView {
  vesselId: number
  vesselName: string
  mmsi?: string
  imo?: string
  riskLevel?: string
  navigationStatus?: string
  status: number
  matchMethod: 'MMSI' | 'IMO' | string
}

export interface ShippingZone {
  id: number
  name: string
  type?: string
  description?: string
  createdAt?: string
}

export type Ecosystem = ShippingZone

export interface AisRecordEnvironment {
  waterDepth?: number | null
  weatherCondition?: string
  seaCondition?: string
}

export interface AisRecordVesselInput {
  vesselId: number | null
  crewCount?: number | null
  behavior?: string
  comment?: string
}

export interface AisRecordVesselView {
  vesselId: number
  vesselName: string
  displayName?: string
  status?: number
  countEstimated?: number
  behavior?: string
  comment?: string
}

export interface AisRecordManualView {
  id: number
  shippingZoneId: number
  shippingZoneName: string
  recorderUserId: number
  recorderName: string
  recordedAt: string
  locationLat: number
  locationLng: number
  locationName?: string
  environmentJson?: string
  note?: string
  createdAt: string
}

export interface AisRecordManualDetailView extends AisRecordManualView {
  linkedVessels: AisRecordVesselView[]
}

export interface ObservationView extends AisRecordManualView {
  ecosystemId: number
  ecosystemName: string
  observerUserId: number
  observerName: string
  observedAt: string
  envJson?: string
}

export interface ObservationSpeciesView {
  vesselId: number
  scientificName?: string
  chineseName?: string
  countEstimated?: number | null
  behavior?: string
  comment?: string
}

export interface ObservationDetailView extends ObservationView {
  speciesItems: ObservationSpeciesView[]
}

export interface AisRecordView {
  id: string
  mmsi: string
  baseDateTime: string
  longitude: number
  latitude: number
  sog?: number | null
  cog?: number | null
  heading?: number | null
  vesselName?: string
  imo?: string
  callSign?: string
  vesselType?: number | null
  status?: number | null
  length?: number | null
  width?: number | null
  draft?: number | null
  cargo?: number | null
  transceiver?: string
  note?: string
  sourceFile?: string
  importedByUserId?: number | null
  importedByName?: string
  importedAt?: string
  linkedVessel?: AisLinkedVesselView | null
}

export interface AisVesselSummaryView {
  totalRecords: number
  firstBaseDateTime?: string
  latestBaseDateTime?: string
  latestRecord?: AisRecordView | null
}

export interface AisDatasetDateStat {
  datasetDate: string
  recordCount: number
}

export interface AisRankingStat {
  label: string
  recordCount: number
}

export interface AisRiskSummary {
  total: number
  lowSpeedCount: number
  stoppedCount: number
  abnormalNoteCount: number
  uniqueVesselCount: number
}

export interface AisImportResult {
  sourceFile: string
  imported: number
  skipped: number
  limit: number
}

export interface AisConvertedCsvSaveResult {
  fileName: string
  savedPath: string
  sizeBytes: number
}

export interface AisImportProgress {
  taskId: string
  sourceFile: string
  status: 'running' | 'completed' | 'failed' | string
  bytesRead: number
  totalBytes: number
  imported: number
  skipped: number
  limit: number
  progress: number
  message: string
  startedAt: string
  updatedAt: string
}

export interface AisVesselDraftBatchRequest {
  keyword?: string
  observedFrom?: string
  observedTo?: string
  limit?: number
}

export interface AisVesselDraftBatchResult {
  scanned: number
  created: number
  skippedExisting: number
  skippedInvalid: number
  limit: number
}

export interface AisBatchOperationResult {
  affected: number
  operation: 'delete' | 'update' | string
}

export interface AisBatchOperationPayload {
  ids?: string[]
  allMatched?: boolean
  keyword?: string
  observedFrom?: string
  observedTo?: string
}

export interface AisBatchUpdatePayload extends AisBatchOperationPayload {
  fields: Record<string, string | number | null>
}

export interface VersionFieldChangeView {
  fieldKey: string
  fieldLabel: string
  oldValue?: string
  newValue?: string
}

export interface EntityVersionView {
  id: number
  versionNo: number
  action: string
  changedBy?: number
  changedByName?: string
  rollbackSourceVersionId?: number
  rollbackSourceVersionNo?: number
  createdAt: string
  changes: VersionFieldChangeView[]
}

export interface AuditLogView {
  id: number
  userId?: number
  username?: string
  displayName?: string
  module: string
  action: string
  entityType?: string
  entityId?: number
  requestId?: string
  success: number
  detailJson?: string
  createdAt: string
}

export interface DashboardSummary {
  totalVesselProfiles: number
  totalAisRecords: number
  totalShippingZones: number
  totalUsers: number
  recentAisRecordCount: number
}

export interface NameValuePoint {
  name: string
  value: number
}

export interface VesselDistributionPoint {
  vesselId: number
  vesselName: string
  displayName?: string
  locationLat: number
  locationLng: number
  routeDescription?: string
  riskLevel?: string
  operationalStatus?: string
}

export interface AisRecordMapPoint {
  recordId: number
  shippingZoneName: string
  recorderName: string
  recordedAt: string
  locationLat: number
  locationLng: number
  locationName?: string
  linkedVesselCount: number
  note?: string
}

export interface ShippingZoneStats {
  zoneId: number
  zoneName: string
  zoneType?: string
  recordCount: number
  linkedVesselCount: number
}

export interface MediaFile {
  id: number
  businessType: string
  businessId: number
  originalFilename: string
  storedFilename: string
  contentType: string
  sizeBytes: number
  storagePath: string
  sha256?: string
  uploadedBy?: number
  uploadedAt: string
}

export interface AiRelatedVesselRecord {
  id: number
  displayName?: string
  profileName: string
  classificationPath?: string
  riskLevel?: string
  operationalStatus?: string
}

export interface AiIdentificationCandidate {
  chineseName?: string
  scientificName?: string
  confidence: number
  reason?: string
}

export interface AiIdentifyImageResponse {
  likelyDisplayName?: string
  likelyProfileName?: string
  confidence: number
  needsHumanReview: boolean
  confidenceLabel: string
  reasoning?: string
  candidates: AiIdentificationCandidate[]
  relatedVesselRecords: AiRelatedVesselRecord[]
  ragEvidence: RagEvidenceItem[]
  confidenceAdjustedByRag: boolean
  ragConclusion?: string
  conflictWarnings: string[]
}

export interface AiVesselAutocompleteResponse {
  chineseName?: string
  scientificName?: string
  phylumName?: string
  className?: string
  orderName?: string
  familyName?: string
  genusName?: string
  riskLevel?: string
  operationalStatus?: string
  description?: string
  morphology?: string
  habit?: string
  habitat?: string
  distribution?: string
  geoRangeText?: string
  summary?: string
  confidence: number
  notes: string[]
  relatedVesselRecords: AiRelatedVesselRecord[]
}

export interface AiPolishTextResponse {
  fieldName: string
  polishedText: string
  summary?: string
  keywords: string[]
}

export interface AiTranslateVesselResponse {
  targetLanguage: string
  description?: string
  morphology?: string
  habit?: string
  habitat?: string
  distribution?: string
  geoRangeText?: string
  summary?: string
}

export interface AiRecordVesselItem {
  vesselId?: number | null
  scientificName?: string
  chineseName?: string
  countEstimated?: number | null
  behavior?: string
  comment?: string
}

export interface AiRecordAnomaly {
  severity: string
  vesselName: string
  message: string
  suggestion?: string
  distanceKm?: number
}

export interface AiRecordAnalysisResponse {
  summary?: string
  tags: string[]
  reviewNotes: string[]
  anomalies: AiRecordAnomaly[]
  needsReview: boolean
}

export interface AiRecordQualityIssue {
  severity: string
  title: string
  message: string
  suggestion?: string
}

export interface AiRecordQualityResponse {
  recordId: number
  score: number
  grade: string
  summary: string
  strengths: string[]
  issues: AiRecordQualityIssue[]
  needsReview: boolean
}

export interface AiAssistantStructuredQuery {
  intent: string
  locationKeyword?: string
  routeKeyword?: string
  vesselKeyword?: string
  riskLevel?: string
  navigationStatus?: string
  yearsBack?: number
  recentDays?: number
  observedFrom?: string
  observedTo?: string
  dateKeyword?: string
  metric?: string
  groupBy?: string
  includeTrend: boolean
  riskOnly: boolean
  limit?: number
}

export interface AiAssistantEvidenceItem {
  type?: string
  title?: string
  description?: string
  sourceId?: number
  score?: number
  sourcePath?: string
}

export interface AiAssistantMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface AiAssistantHistoryItem extends AiAssistantMessage {
  id: number
  createdAt: string
}

export interface AiAssistantChatResponse {
  answer: string
  structuredQuery: AiAssistantStructuredQuery
  highlights: string[]
  evidence: AiAssistantEvidenceItem[]
  cacheHit: boolean
}

export interface AiAssistantHistoryResponse {
  messages: AiAssistantHistoryItem[]
  lastResponse?: AiAssistantChatResponse
}

export interface AiAssistantStreamEvent {
  type: 'status' | 'delta' | 'final' | 'error'
  content?: string
  response?: AiAssistantChatResponse
}

export interface AiReviewTicketView {
  id: number
  sourceType: string
  status: string
  resolutionCode?: string
  submittedBy: number
  submittedByName: string
  reviewerUserId?: number
  reviewerName?: string
  likelyDisplayName?: string
  likelyProfileName?: string
  confidence: number
  needsHumanReview: boolean
  imageMediaId?: number
  imageUrl?: string
  reviewedAt?: string
  createdAt: string
  updatedAt: string
}

export interface AiReviewTicketDetailView extends AiReviewTicketView {
  imageMediaId?: number
  reasoning?: string
  candidates: AiIdentificationCandidate[]
  relatedVesselRecords: AiRelatedVesselRecord[]
  ragEvidence: RagEvidenceItem[]
  initialRecognitionJson?: string
  reviewEvidenceJson?: string
  submitNote?: string
  finalVesselId?: number
  finalChineseName?: string
  finalScientificName?: string
  reviewNote?: string
}

export interface AiReportView {
  id: number
  reportType: string
  days: number
  periodStart?: string
  periodEnd?: string
  title: string
  summary: string
  createdBy: number
  creatorName: string
  createdAt: string
}

export interface AiReportDetailView extends AiReportView {
  highlights: string[]
  risks: string[]
  recommendations: string[]
  evidence: string[]
  metrics?: AiReportMetrics
}

export interface AiReportMetrics {
  periodStart?: string
  periodEnd?: string
  latestDatasetDate?: string
  totalRecords: number
  uniqueVesselCount: number
  lowSpeedCount: number
  stoppedCount: number
  abnormalNoteCount: number
  riskSignalCount: number
  topDates: AiReportDateStat[]
  topImporters: AiReportRankingStat[]
}

export interface AiReportDateStat {
  datasetDate: string
  recordCount: number
}

export interface AiReportRankingStat {
  label: string
  recordCount: number
}

export interface RagDocumentView {
  id: number
  sourceType: string
  sourceId?: number
  mediaId?: number
  title: string
  originalFilename?: string
  contentType?: string
  status: string
  chunkCount: number
  errorMessage?: string
  uploadedBy?: number
  createdAt: string
  updatedAt: string
}

export interface RagChunkView {
  id: number
  documentId: number
  sourceType: string
  sourceId?: number
  chunkIndex: number
  title: string
  summary?: string
  content: string
  vectorPointId?: string
  embeddingStatus?: string
  embeddingError?: string
  characterCount: number
  status: string
  createdAt: string
}

export interface RagEvidenceItem {
  sourceType: string
  sourceId?: number
  documentId: number
  chunkId: number
  title: string
  summary?: string
  contentSnippet?: string
  score: number
  sourcePath?: string
  sourceName?: string
  scenario?: string
}

export interface RagDocumentDetailView {
  document: RagDocumentView
  chunks: RagChunkView[]
}

export interface RagIndexJobView {
  id: number
  jobType: string
  status: string
  targetSourceType?: string
  targetSourceId?: number
  totalDocuments: number
  totalChunks: number
  successCount: number
  failedCount: number
  errorMessage?: string
  startedAt: string
  finishedAt?: string
  createdBy?: number
  createdAt: string
}

export interface RagSearchResultView {
  chunkId: number
  documentId: number
  sourceType: string
  sourceId?: number
  title: string
  summary?: string
  content: string
  score: number
  cosineScore: number
  keywordScore: number
  sourcePath?: string
}

export interface RagIngestJobView {
  id: number
  jobType: string
  status: string
  sourceCode?: string
  title?: string
  totalItems: number
  processedItems: number
  successCount: number
  failedCount: number
  errorMessage?: string
  createdBy?: number
  startedAt: string
  finishedAt?: string
  createdAt: string
}

export interface RagIngestItemView {
  id: number
  jobId: number
  sourceType: string
  sourceCode?: string
  externalId?: string
  sourceUrl?: string
  localPath?: string
  mediaId?: number
  ragDocumentId?: number
  title?: string
  status: string
  errorMessage?: string
  createdAt: string
  updatedAt: string
}

export interface RagSourceView {
  id: number
  code: string
  name: string
  sourceType: string
  baseUrl?: string
  enabled: boolean
}

export interface QdrantStatusView {
  available: boolean
  status: string
  pointsCount: number
  readyChunks: number
  errorMessage?: string
}

// ==================== Quiz 知识问答 ====================

export interface QuizQuestion {
  id: number
  category: string
  type: string
  title: string
  options: string
  answer: string
  explanation?: string
  difficulty: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface QuizRecord {
  id: number
  userId: number
  score: number
  total: number
  categories?: string
  mode: string
  startedAt: string
  finishedAt?: string
}
