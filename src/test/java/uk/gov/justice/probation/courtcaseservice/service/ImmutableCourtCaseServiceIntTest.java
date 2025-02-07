package uk.gov.justice.probation.courtcaseservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.CannotAcquireLockException;
import uk.gov.justice.probation.courtcaseservice.BaseIntTest;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.CourtCaseEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.EntityHelper;
import uk.gov.justice.probation.courtcaseservice.jpa.repository.CourtRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImmutableCourtCaseServiceIntTest extends BaseIntTest {
    private static final String CRN = "CRN";
    private static final CourtCaseEntity COURT_CASE_ENTITY = EntityHelper.aCourtCaseEntityWithCrn(CRN);
    private static final String COURT_CODE = "B10JQ";
    @Autowired
    private ImmutableCourtCaseService courtCaseService;
    @MockBean
    private CourtRepository courtRepository;

    @Test
    public void givenCannotAcquireLockExceptionThrown_whenCreateCase_thenRetry() {
        when(courtRepository.findByCourtCode(COURT_CODE)).thenThrow(CannotAcquireLockException.class);
        assertThatExceptionOfType(CannotAcquireLockException.class)
                .isThrownBy(() -> courtCaseService.createCase("1234", COURT_CASE_ENTITY));

        verify(courtRepository, times(3)).findByCourtCode(COURT_CODE);
    }

    @Test
    public void givenCannotAcquireLockExceptionThrown_whenCreateUpdateCaseForSingleDefendantId_thenRetry() {
        when(courtRepository.findByCourtCode(COURT_CODE)).thenThrow(CannotAcquireLockException.class);
        assertThatExceptionOfType(CannotAcquireLockException.class)
                .isThrownBy(() -> courtCaseService.createUpdateCaseForSingleDefendantId("1234", "12345", COURT_CASE_ENTITY));

        verify(courtRepository, times(3)).findByCourtCode(COURT_CODE);
    }
}
