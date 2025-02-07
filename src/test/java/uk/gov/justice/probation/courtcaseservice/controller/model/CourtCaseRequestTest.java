package uk.gov.justice.probation.courtcaseservice.controller.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.AddressPropertiesEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.DefendantOffenceEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.DefendantType;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.OffenceEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.Sex;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.SourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.AWAITING_PSR;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.BREACH;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.CRN;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.CRO;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.DEFENDANT_DOB;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.DEFENDANT_ID;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.DEFENDANT_NAME;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.DEFENDANT_SEX;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.NAME;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.NATIONALITY_1;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.NATIONALITY_2;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.NO_RECORD_DESCRIPTION;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.PNC;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.PRE_SENTENCE_ACTIVITY;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.SUSPENDED_SENTENCE;
import static uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper.TERMINATION_DATE;

class CourtCaseRequestTest {

    private static final LocalDateTime SESSION_START_TIME = LocalDateTime.of(2020, 10, 5, 9, 30);

    @Test
    void givenAllFieldsPresent_whenCallAsEntity_returnCourtCaseEntity() {
        final var request = CourtCaseRequest.builder()
                                                            .caseId("CASE_ID")
                                                            .caseNo("CASE_NO")
                                                            .courtCode("COURT_CODE")
                                                            .courtRoom("COURT_ROOM")
                                                            .source("LIBRA")
                                                            .sessionStartTime(SESSION_START_TIME)
                                                            .probationStatus(NO_RECORD_DESCRIPTION)
                                                            .previouslyKnownTerminationDate(TERMINATION_DATE)
                                                            .suspendedSentenceOrder(SUSPENDED_SENTENCE)
                                                            .breach(BREACH)
                                                            .preSentenceActivity(PRE_SENTENCE_ACTIVITY)
                                                            .offences(Arrays.asList(
                                                                    new OffenceRequestResponse("OFFENCE_TITLE1", "OFFENCE_SUMMARY1","ACT1"),
                                                                    new OffenceRequestResponse("OFFENCE_TITLE2", "OFFENCE_SUMMARY2","ACT2")
                                                                )
                                                            )
                                                            .defendantDob(DEFENDANT_DOB)
                                                            .defendantId(DEFENDANT_ID)
                                                            .name(NAME)
                                                            .defendantName(NAME.getFullName())
                                                            .defendantAddress(new AddressRequestResponse("LINE1", "LINE2", "LINE3", "LINE4", "LINE5", "POSTCODE"))
                                                            .defendantSex(DEFENDANT_SEX)
                                                            .defendantType(DefendantType.PERSON)
                                                            .crn(CRN)
                                                            .pnc(PNC)
                                                            .cro(CRO)
                                                            .listNo("LIST_NO")
                                                            .nationality1(NATIONALITY_1)
                                                            .nationality2(NATIONALITY_2)
                                                            .awaitingPsr(AWAITING_PSR)
                                                            .build();

        final var entity = request.asEntity();

        assertThat(entity.getCaseId()).isEqualTo("CASE_ID");
        assertThat(entity.getCaseNo()).isEqualTo("CASE_NO");
        assertThat(entity.getSourceType()).isSameAs(SourceType.LIBRA);
        assertThat(entity.getProbationStatus()).isEqualTo(ProbationStatus.NO_RECORD.getName());
        assertThat(entity.getPreviouslyKnownTerminationDate()).isEqualTo(TERMINATION_DATE);
        assertThat(entity.getSuspendedSentenceOrder()).isEqualTo(SUSPENDED_SENTENCE);
        assertThat(entity.getBreach()).isEqualTo(BREACH);
        assertThat(entity.getPreSentenceActivity()).isEqualTo(PRE_SENTENCE_ACTIVITY);
        assertThat(entity.getName()).isEqualTo(NAME);
        assertThat(entity.getDefendantName()).isEqualTo(NAME.getFullName());
        assertThat(entity.getDefendantAddress().getLine1()).isEqualTo("LINE1");
        assertThat(entity.getDefendantAddress().getLine2()).isEqualTo("LINE2");
        assertThat(entity.getDefendantAddress().getLine3()).isEqualTo("LINE3");
        assertThat(entity.getDefendantAddress().getLine4()).isEqualTo("LINE4");
        assertThat(entity.getDefendantAddress().getLine5()).isEqualTo("LINE5");
        assertThat(entity.getDefendantAddress().getPostcode()).isEqualTo("POSTCODE");
        assertThat(entity.getDefendantDob()).isEqualTo(DEFENDANT_DOB);
        assertThat(entity.getDefendantSex()).isEqualTo(Sex.MALE);
        assertThat(entity.getCrn()).isEqualTo(CRN);
        assertThat(entity.getPnc()).isEqualTo(PNC);
        assertThat(entity.getCro()).isEqualTo(CRO);
        assertThat(entity.getNationality1()).isEqualTo(NATIONALITY_1);
        assertThat(entity.getNationality2()).isEqualTo(NATIONALITY_2);
        assertThat(entity.getAwaitingPsr()).isEqualTo(AWAITING_PSR);
        var expectedOffenceEntity1 = OffenceEntity.builder()
                .offenceSummary("OFFENCE_SUMMARY1")
                .offenceTitle("OFFENCE_TITLE1")
                .act("ACT1")
                .sequenceNumber(1)
                .build();
        var expectedOffenceEntity2 = OffenceEntity.builder()
            .offenceSummary("OFFENCE_SUMMARY2")
            .offenceTitle("OFFENCE_TITLE2")
            .act("ACT2")
            .sequenceNumber(2)
            .build();
        assertThat(entity.getOffences().get(0)).usingRecursiveComparison()
            .ignoringFields("courtCase", "id")
            .isEqualTo(expectedOffenceEntity1);
        assertThat(entity.getOffences().get(1)).usingRecursiveComparison()
            .ignoringFields("courtCase", "id")
            .isEqualTo(expectedOffenceEntity2);

        assertThat(entity.getHearings()).hasSize(1);
        assertThat(entity.getDefendants()).hasSize(1);
        final var defendant = entity.getDefendants().get(0);

        final var address = AddressPropertiesEntity.builder()
            .line1("LINE1")
            .line2("LINE2")
            .line3("LINE3")
            .line4("LINE4")
            .line5("LINE5")
            .postcode("POSTCODE")
            .build();
        final var expectedDefendant = EntityHelper.aDefendantEntity(address).withProbationStatus(ProbationStatus.NO_RECORD.name());
        assertThat(defendant)
            .usingRecursiveComparison()
            .ignoringFields("id", "courtCase", "offences", "offender")
            .isEqualTo(expectedDefendant);
        assertThat(defendant.getCourtCase()).isNotNull();

        assertThat(defendant.getOffences()).hasSize(2);
        var expectedDefendantOffenceEntity1 = DefendantOffenceEntity.builder()
            .summary("OFFENCE_SUMMARY1")
            .title("OFFENCE_TITLE1")
            .act("ACT1")
            .sequence(1)
            .build();
        assertThat(defendant.getOffences().get(0))
            .usingRecursiveComparison()
            .ignoringFields("id", "defendant")
            .isEqualTo(expectedDefendantOffenceEntity1);
        assertThat(defendant.getOffences().get(0).getDefendant()).isNotNull();

        assertThat(defendant.getOffender().getCrn()).isEqualTo(CRN);
        assertThat(defendant.getOffender().getProbationStatus()).isSameAs(ProbationStatus.NO_RECORD);
        assertThat(defendant.getOffender().getPreviouslyKnownTerminationDate()).isEqualTo(TERMINATION_DATE);
        assertThat(defendant.getOffender().getAwaitingPsr()).isEqualTo(AWAITING_PSR);
        assertThat(defendant.getOffender().isBreach()).isEqualTo(BREACH);
        assertThat(defendant.getOffender().isPreSentenceActivity()).isEqualTo(PRE_SENTENCE_ACTIVITY);
        assertThat(defendant.getOffender().isSuspendedSentenceOrder()).isEqualTo(SUSPENDED_SENTENCE);
    }

    @Test
    void givenOptionalFieldsNotPresent_whenCallAsEntity_returnCourtCaseEntity() {
        final var request = CourtCaseRequest.builder()
            .caseId("CASE_ID")
            .caseNo("CASE_NO")
            .courtCode("COURT_CODE")
            .courtRoom("COURT_ROOM")
            .sessionStartTime(SESSION_START_TIME)
            .probationStatus("PROBATION_STATUS")
            .defendantName(DEFENDANT_NAME)
            .defendantDob(DEFENDANT_DOB)
            .build();

        final var entity = request.asEntity();

        assertThat(entity.getCaseId()).isEqualTo("CASE_ID");
        assertThat(entity.getCaseNo()).isEqualTo("CASE_NO");
        assertThat(entity.getSourceType()).isSameAs(SourceType.LIBRA);
        assertThat(entity.getProbationStatus()).isEqualTo("PROBATION_STATUS");

        assertThat(entity.getHearings()).hasSize(1);
        assertThat(entity.getDefendants()).hasSize(1);
        assertThat(entity.getDefendants().get(0).getDefendantId()).isNotNull();
        assertThat(entity.getDefendants().get(0).getDefendantName()).isEqualTo(DEFENDANT_NAME);
        assertThat(entity.getDefendants().get(0).getDateOfBirth()).isEqualTo(DEFENDANT_DOB);
        assertThat(entity.getDefendants().get(0).getOffender()).isNull();
    }

    @Test
    void givenOptionalOffenderFieldsNotPresent_whenCallAsEntity_returnCourtCaseEntityWithOffenderDefaults() {
        final var request = CourtCaseRequest.builder()
            .caseId("CASE_ID")
            .caseNo("CASE_NO")
            .courtCode("COURT_CODE")
            .courtRoom("COURT_ROOM")
            .sessionStartTime(SESSION_START_TIME)
            .probationStatus("PROBATION_STATUS")
            .defendantName(DEFENDANT_NAME)
            .defendantDob(DEFENDANT_DOB)
            .crn("CRN")
                .previouslyKnownTerminationDate(LocalDate.of(2021, 1, 1))
            .build();

        final var entity = request.asEntity();

        final var offender = entity.getDefendants().get(0).getOffender();
        assertThat(offender).isNotNull();
        assertThat(offender.isBreach()).isFalse();
        assertThat(offender.isPreSentenceActivity()).isFalse();
        assertThat(offender.isSuspendedSentenceOrder()).isFalse();
    }
}
