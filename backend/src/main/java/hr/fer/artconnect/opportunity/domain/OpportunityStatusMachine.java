package hr.fer.artconnect.opportunity.domain;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.OffsetDateTime;

/** Pravila dijele ručna promjena statusa i posao automatskog zatvaranja. */
public final class OpportunityStatusMachine {
    private OpportunityStatusMachine() {}

    public static void validate(OpportunityStatus current, OpportunityStatus target, boolean archived,
            OffsetDateTime deadline, OffsetDateTime now, boolean organizerApproved) {
        boolean allowed = switch (current) {
            case PREPARATION -> target == OpportunityStatus.OPEN;
            case OPEN -> target == OpportunityStatus.CLOSED;
            case CLOSED -> target == OpportunityStatus.OPEN || target == OpportunityStatus.SELECTION;
            case SELECTION, FINISHED -> false;
        };
        if (archived || !allowed) {
            throw new ApiException(ErrorCode.INVALID_STATUS_TRANSITION, "Promjena statusa prilike nije dopuštena.");
        }
        if (current == OpportunityStatus.PREPARATION && !organizerApproved) {
            throw new ApiException(ErrorCode.ORGANIZER_NOT_APPROVED, "Organizator još nije odobren za objavu prilika.");
        }
        if (target == OpportunityStatus.OPEN && !deadline.isAfter(now)) {
            throw new ApiException(ErrorCode.DEADLINE_PASSED, "Rok prijave je istekao.");
        }
    }
}
