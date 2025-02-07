package uk.gov.justice.probation.courtcaseservice.service;


import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.probation.courtcaseservice.controller.model.GroupedOffenderMatchesRequest;
import uk.gov.justice.probation.courtcaseservice.controller.model.OffenderMatchDetail;
import uk.gov.justice.probation.courtcaseservice.controller.model.ProbationStatus;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.CourtCaseEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.GroupedOffenderMatchesEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.OffenderMatchEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.repository.GroupedOffenderMatchRepository;
import uk.gov.justice.probation.courtcaseservice.restclient.OffenderRestClient;
import uk.gov.justice.probation.courtcaseservice.restclient.OffenderRestClientFactory;
import uk.gov.justice.probation.courtcaseservice.restclient.exception.OffenderNotFoundException;
import uk.gov.justice.probation.courtcaseservice.service.model.Conviction;
import uk.gov.justice.probation.courtcaseservice.service.model.ProbationStatusDetail;
import uk.gov.justice.probation.courtcaseservice.service.model.Sentence;

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderMatchServiceTest {
    public static final String COURT_CODE = "B10JQ";
    public static final String CASE_NO = "123456789";

    @Mock
    private OffenderRestClientFactory offenderRestClientFactory;
    @Mock
    private OffenderRestClient offenderRestClient;
    @Mock
    private CourtCaseService courtCaseService;
    @Mock
    private GroupedOffenderMatchRepository offenderMatchRepository;
    @Mock
    private GroupedOffenderMatchesEntity groupedOffenderMatchesEntity;
    @Mock
    private GroupedOffenderMatchesRequest groupedOffenderMatchesRequest;
    @Mock
    private CourtCaseEntity courtCaseEntity;

    private OffenderMatchService service;

    @BeforeEach
    void setUp() {
        when(offenderRestClientFactory.build()).thenReturn(offenderRestClient);
        service = new OffenderMatchService(courtCaseService, offenderMatchRepository, offenderRestClientFactory);
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class CreateOrUpdateByCaseId {

        private static final String CASE_ID = "f1e1867f-94a5-45a2-81cf-92780a51564d";
        private static final String DEFENDANT_ID = "378752d2-2a60-42d9-8f70-89d6fa022be4";

        GroupedOffenderMatchesRequest request = GroupedOffenderMatchesRequest.builder()
            .matches(Collections.emptyList())
            .build();

        @Test
        void givenNoExistingCase_whenCreateOrUpdate_thenCreate() {
            when(offenderMatchRepository.findByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID)).thenReturn(Optional.empty());
            when(courtCaseEntity.getCaseId()).thenReturn(CASE_ID);
            when(courtCaseService.getCaseByCaseId(CASE_ID)).thenReturn(courtCaseEntity);
            when(offenderMatchRepository.save(argThat(new EntityMatcher(DEFENDANT_ID, CASE_ID)))).thenReturn(groupedOffenderMatchesEntity);

            var match = service.createOrUpdateGroupedMatchesByDefendant(CASE_ID, DEFENDANT_ID, request).blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get()).isEqualTo(groupedOffenderMatchesEntity);
        }

        @Test
        void givenAnExistingCase_whenCreateOrUpdate_thenUpdate() {

            // Group has no defendant ID to start with. Prove that the update has happened by asserting on it later
            var groupEntity = GroupedOffenderMatchesEntity.builder().caseId(CASE_ID).offenderMatches(Collections.emptyList()).build();
            when(offenderMatchRepository.findByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID)).thenReturn(Optional.of(groupEntity));
            when(offenderMatchRepository.save(groupEntity)).thenReturn(groupEntity);

            var match = service.createOrUpdateGroupedMatchesByDefendant(CASE_ID, DEFENDANT_ID, request).blockOptional();

            assertThat(match).isPresent();
            assertThat(match.get()).isEqualTo(groupEntity);
            assertThat(groupEntity.getDefendantId()).isEqualTo(DEFENDANT_ID);
        }

        @Data
        class EntityMatcher implements ArgumentMatcher<GroupedOffenderMatchesEntity> {

            private final String defendantId;
            private final String caseId;

            @Override
            public boolean matches(GroupedOffenderMatchesEntity argument) {
                return defendantId.equals(argument.getDefendantId()) && caseId.equals(argument.getCaseId());
            }
        }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class GetSentence {

        @Test
        void givenMultipleConvictionsIncludingNullDates_whenGetMostRecentSentence_theReturn() {
            LocalDate date20July = LocalDate.of(2020, Month.JULY, 20);
            LocalDate date21July = LocalDate.of(2020, Month.JULY, 21);
            Sentence sentence3 = Sentence.builder().description("B").build();
            Conviction conviction1 = Conviction.builder().sentence(Sentence.builder().description("A").build()).build();
            Conviction conviction2 = Conviction.builder().convictionDate(date20July).sentence(Sentence.builder().description("B").build()).build();
            Conviction conviction3 = Conviction.builder().convictionDate(date21July).sentence(sentence3).build();

            Sentence sentence = service.getSentenceForMostRecentConviction(List.of(conviction1, conviction2, conviction3));

            assertThat(sentence).isSameAs(sentence3);
        }

        @Test
        void givenConvictionWithNoSentence_whenGetMostRecentSentence_thenReturnNull() {
            Sentence sentence = service.getSentenceForMostRecentConviction(List.of(Conviction.builder().build()));

            assertThat(sentence).isNull();
        }

        @Test
        void givenNullInput_whenGetMostRecentSentence_thenReturnNull() {
            assertThat(service.getSentenceForMostRecentConviction(null)).isNull();
        }

    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class GetOffenderMatchDetail {
        private static final String CASE_ID = "4d113429-e38c-4fbf-bd94-e1c3569319eb";
        private static final String DEFENDANT_ID = "e19b2776-6646-4940-93af-6b86fa1b7416";
        private Conviction activeConviction;
        private Conviction inactiveConviction;

        @BeforeEach
        void beforeEach() {
            this.activeConviction = buildConviction(true, "sentence1");
            this.inactiveConviction = buildConviction(false, "sentence2");
        }

        @Test
        void whenGetOffenderMatchDetail_thenReturn() {
            final var matchDetail = OffenderMatchDetail.builder().forename("Chris").build();
            final var crn = "X320741";
            mockOffenderDetailMatch(crn, matchDetail, List.of(activeConviction));

            final var offenderMatchDetail = service.getOffenderMatchDetail(crn);

            verify(offenderRestClient).getOffenderMatchDetailByCrn(crn);
            verify(offenderRestClient).getConvictionsByCrn(crn);
            assertThat(offenderMatchDetail.getForename()).isEqualTo("Chris");
            assertThat(offenderMatchDetail.getProbationStatus()).isEqualTo(ProbationStatus.CURRENT);
        }

        @Test
        void givenNoConvictions_whenGetOffenderMatchDetail_thenReturn() {

            final var matchDetail = OffenderMatchDetail.builder().forename("Chris").build();
            final var crn = "X320741";
            mockOffenderDetailMatch(crn, matchDetail, Collections.emptyList());

            final var offenderMatchDetail = service.getOffenderMatchDetail("X320741");

            assertThat(offenderMatchDetail.getForename()).isEqualTo("Chris");
            verify(offenderRestClient).getOffenderMatchDetailByCrn(crn);
            verify(offenderRestClient).getConvictionsByCrn(crn);
            assertThat(offenderMatchDetail.getProbationStatus()).isEqualTo(ProbationStatus.CURRENT);
        }

        @Test
        void given404OnConvictionsCall_whenGetOffenderMatchDetail_thenReturn() {

            final var matchDetail = OffenderMatchDetail.builder().forename("Chris").build();
            final var crn = "X320741";
            when(offenderRestClient.getOffenderMatchDetailByCrn(crn)).thenReturn(Mono.justOrEmpty(matchDetail));
            when(offenderRestClient.getConvictionsByCrn(crn)).thenReturn(Mono.error(new OffenderNotFoundException(crn)));
            when(offenderRestClient.getProbationStatusByCrn(crn)).thenReturn(Mono.just(ProbationStatusDetail.builder().status("CURRENT").build()));

            final var offenderMatchDetail = service.getOffenderMatchDetail("X320741");

            assertThat(offenderMatchDetail.getForename()).isEqualTo("Chris");
            assertThat(offenderMatchDetail.getProbationStatus()).isEqualTo(ProbationStatus.CURRENT);
        }

        @Test
        void givenNoMatchDetail_whenGetOffenderMatchDetail_thenReturnNull() {

            String crn = "X320741";
            mockOffenderDetailMatch(crn, null, List.of(activeConviction));

            final var offenderMatchDetail = service.getOffenderMatchDetail("X320741");

            assertThat(offenderMatchDetail).isNull();
            verify(offenderRestClient).getOffenderMatchDetailByCrn(crn);
            verify(offenderRestClient).getConvictionsByCrn(crn);
            verify(offenderRestClient).getProbationStatusByCrn(crn);
        }

        @Test
        void givenMultipleCrns_whenGetOffenderMatchDetailsByCaseAndDefendantId_thenReturn() {

            String crn1 = "X320741";
            String crn2 = "X320742";

            when(offenderMatchRepository.findByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID)).thenReturn(
                Optional.ofNullable(buildGroupedOffenderMatchesEntity(List.of(crn1, crn2))));

            final var matchDetail1 = OffenderMatchDetail.builder().forename("Chris").build();
            final var matchDetail2 = OffenderMatchDetail.builder().forename("Dave").build();

            mockOffenderDetailMatch(crn1, matchDetail1, Collections.emptyList());
            mockOffenderDetailMatch(crn2, matchDetail2, List.of(inactiveConviction, activeConviction));

            final var response = service.getOffenderMatchDetailsByCaseIdAndDefendantId(CASE_ID, DEFENDANT_ID);

            assertThat(response.getOffenderMatchDetails()).hasSize(2);
            assertThat(response.getOffenderMatchDetails()).extracting("forename").containsExactlyInAnyOrder("Chris", "Dave");
        }

        private GroupedOffenderMatchesEntity buildGroupedOffenderMatchesEntity(List<String> crns) {

            List<OffenderMatchEntity> offenderMatchEntities = crns.stream()
                .map(crn -> OffenderMatchEntity.builder().crn(crn).build())
                .collect(Collectors.toList());

            return GroupedOffenderMatchesEntity.builder().offenderMatches(offenderMatchEntities).build();
        }

        private void mockOffenderDetailMatch(String crn, OffenderMatchDetail matchDetail, List<Conviction> convictions) {
            when(offenderRestClient.getOffenderMatchDetailByCrn(crn)).thenReturn(Mono.justOrEmpty(matchDetail));
            when(offenderRestClient.getConvictionsByCrn(crn)).thenReturn(Mono.just(convictions));
            when(offenderRestClient.getProbationStatusByCrn(crn)).thenReturn(Mono.just(ProbationStatusDetail.builder().status("CURRENT").build()));
        }

        private Conviction buildConviction(boolean active, String sentenceDesc) {
            LocalDate date = LocalDate.of(2020, Month.JULY, 20);
            Sentence sentence = Sentence.builder().description(sentenceDesc).build();
            return Conviction.builder()
                .active(active)
                .convictionDate(date)
                .sentence(sentence)
                .build();
        }
    }

}
