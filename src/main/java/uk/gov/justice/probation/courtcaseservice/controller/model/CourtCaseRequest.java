package uk.gov.justice.probation.courtcaseservice.controller.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.AddressPropertiesEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.CourtCaseEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.DefendantEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.DefendantOffenceEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.DefendantType;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.HearingEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.NamePropertiesEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.OffenceEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.OffenderEntity;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.Sex;
import uk.gov.justice.probation.courtcaseservice.jpa.entity.SourceType;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
public class CourtCaseRequest {
    static final SourceType DEFAULT_SOURCE = SourceType.LIBRA;
    @NotBlank
    private final String caseId;
    private final String caseNo;
    @NotBlank
    private final String courtCode;
    @NotBlank
    private final String courtRoom;
    private final String source;
    @NotNull
    private final LocalDateTime sessionStartTime;
    private final String probationStatus;
    private final LocalDate previouslyKnownTerminationDate;
    private final Boolean suspendedSentenceOrder;
    private final Boolean breach;
    private final Boolean preSentenceActivity;
    @Valid
    @NotEmpty
    private final List<OffenceRequestResponse> offences;
    @NotNull
    private final NamePropertiesEntity name;
    @NotBlank
    private final String defendantName;
    private final AddressRequestResponse defendantAddress;
    private final LocalDate defendantDob;
    private final String defendantSex;
    @NotNull
    private final DefendantType defendantType;
    // This may be null in the CourtCaseRequest because it is assigned when null before saving
    private final String defendantId;
    private final String crn;
    private final String pnc;
    private final String cro;
    private final String listNo;
    private final String nationality1;
    private final String nationality2;
    private final Boolean awaitingPsr;

    public CourtCaseEntity asEntity() {
        final List<OffenceEntity> offences = IntStream.range(0, Optional.ofNullable(getOffences())
                .map(List::size)
                .orElse(0)
        )
                .mapToObj(i -> {
                    var offence = getOffences().get(i);
                    return OffenceEntity.builder()
                        .sequenceNumber(i + 1)
                        .offenceTitle(offence.getOffenceTitle())
                        .offenceSummary(offence.getOffenceSummary())
                        .act(offence.getAct())
                        .build();
                })
                .collect(Collectors.toList());

        final List<HearingEntity> hearings = List.of(HearingEntity.builder()
            .courtCode(courtCode)
            .courtRoom(courtRoom)
            .hearingDay(sessionStartTime.toLocalDate())
            .hearingTime(sessionStartTime.toLocalTime())
            .listNo(listNo)
            .build());

        final List<DefendantEntity> defendants = buildDefendants(offences);

        final CourtCaseEntity entity = CourtCaseEntity.builder()
                .caseId(caseId)
                .caseNo(caseNo)
                .sourceType(SourceType.valueOf(Optional.ofNullable(source).orElse(DEFAULT_SOURCE.name())))
                .probationStatus(probationStatus)
                .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
                .suspendedSentenceOrder(suspendedSentenceOrder)
                .breach(breach)
                .preSentenceActivity(preSentenceActivity)
                .defendantName(defendantName)
                .defendantDob(defendantDob)
                .defendantSex(Sex.fromString(defendantSex))
                .defendantType(defendantType)
                .name(Optional.ofNullable(name)
                        .map(nameRequest -> NamePropertiesEntity.builder()
                            .title(name.getTitle())
                            .forename1(name.getForename1())
                            .forename2(name.getForename2())
                            .forename3(name.getForename3())
                            .surname(name.getSurname())
                            .build()
                        ).orElse(null) )
                .crn(crn)
                .pnc(pnc)
                .cro(cro)
                .nationality1(nationality1)
                .nationality2(nationality2)
                .offences(offences)
                .hearings(hearings)
                .defendants(defendants)
                .defendantAddress(Optional.ofNullable(defendantAddress)
                        .map(addressRequest -> AddressPropertiesEntity.builder()
                                .line1(defendantAddress.getLine1())
                                .line2(defendantAddress.getLine2())
                                .line3(defendantAddress.getLine3())
                                .line4(defendantAddress.getLine4())
                                .line5(defendantAddress.getLine5())
                                .postcode(defendantAddress.getPostcode())
                            .build()
                        ).orElse(null))
                .awaitingPsr(awaitingPsr)
                .build();

        offences.forEach(offence -> offence.setCourtCase(entity));
        hearings.forEach(hearingEntity -> hearingEntity.setCourtCase(entity));
        defendants.forEach(defendantEntity -> defendantEntity.setCourtCase(entity));
        return entity;
    }

    List<DefendantEntity> buildDefendants(final List<OffenceEntity> caseOffences) {

        final List<DefendantOffenceEntity> defendantOffences = Optional.ofNullable(caseOffences).orElse(Collections.emptyList())
                                                        .stream()
                                                        .map(offence -> DefendantOffenceEntity.builder()
                                                                            .sequence(offence.getSequenceNumber())
                                                                            .title(offence.getOffenceTitle())
                                                                            .summary(offence.getOffenceSummary())
                                                                            .act(offence.getAct())
                                                                            .build())
                                                        .collect(Collectors.toList());

        final var defendant = DefendantEntity.builder()
            .address(Optional.ofNullable(defendantAddress)
                    .map(this::buildAddress)
                    .orElse(null))
            .offender(Optional.ofNullable(crn)
                .map(this::buildOffender)
                .orElse(null))
            .dateOfBirth(defendantDob)
            .defendantName(defendantName)
            .type(defendantType)
            .nationality1(nationality1)
            .nationality2(nationality2)
            .name(name)
            .sex(Sex.fromString(defendantSex))
            .defendantId(Optional.ofNullable(defendantId).orElse(UUID.randomUUID().toString()))
            .cro(cro)
            .pnc(pnc)
            .awaitingPsr(awaitingPsr)
            .probationStatus(ProbationStatus.of(probationStatus).name())
            .preSentenceActivity(preSentenceActivity)
            .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
            .breach(breach)
            .suspendedSentenceOrder(suspendedSentenceOrder)
            .offences(defendantOffences)
            .build();

        defendantOffences.forEach(defendantOffence -> defendantOffence.setDefendant(defendant));
        return Collections.singletonList(defendant);
    }

    private AddressPropertiesEntity buildAddress(AddressRequestResponse addressRequest) {
        return AddressPropertiesEntity.builder()
            .line1(addressRequest.getLine1())
            .line2(addressRequest.getLine2())
            .line3(addressRequest.getLine3())
            .line4(addressRequest.getLine4())
            .line5(addressRequest.getLine5())
            .postcode(addressRequest.getPostcode())
            .build();
    }

    private OffenderEntity buildOffender(String crn) {
        return OffenderEntity.builder()
                .crn(crn)
                .probationStatus(ProbationStatus.of(probationStatus))
                .previouslyKnownTerminationDate(previouslyKnownTerminationDate)
                .awaitingPsr(awaitingPsr)
                .breach(Optional.ofNullable(breach).orElse(false))
                .preSentenceActivity(Optional.ofNullable(preSentenceActivity).orElse(false))
                .suspendedSentenceOrder(Optional.ofNullable(suspendedSentenceOrder).orElse(false))
                .build();
    }

}
