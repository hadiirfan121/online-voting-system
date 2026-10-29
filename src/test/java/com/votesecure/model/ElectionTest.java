package com.votesecure.model;

import com.votesecure.exception.ValidationException;
import com.votesecure.util.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElectionTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 28, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 28, 17, 0);

    @Test
    @DisplayName("Election status uses start-inclusive and end-exclusive boundaries")
    void statusBoundaries() {
        Election election = new Election("E1", "Test Election", START, END);
        assertEquals(ElectionStatus.NOT_STARTED, election.statusAt(START.minusSeconds(1)));
        assertEquals(ElectionStatus.OPEN, election.statusAt(START));
        assertEquals(ElectionStatus.OPEN, election.statusAt(END.minusSeconds(1)));
        assertEquals(ElectionStatus.CLOSED, election.statusAt(END));
        assertEquals(ElectionStatus.CLOSED, election.statusAt(END.plusDays(1)));
    }

    @Test
    @DisplayName("An election end time must be after its start time")
    void invalidRangeIsRejectedByTheModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new Election("E1", "Bad Election", START, START));
        assertThrows(IllegalArgumentException.class,
                () -> new Election("E1", "Bad Election", START, START.minusMinutes(1)));
    }

    @Test
    @DisplayName("The same chronological rule applies in the validation utility")
    void invalidRangeIsRejectedByValidation() {
        assertThrows(ValidationException.class, () -> Validation.requireChronologicalWindow(START, START));
        assertThrows(ValidationException.class, () -> Validation.requireChronologicalWindow(START, END.minusYears(1)));
    }

    @Test
    @DisplayName("An updated window keeps the same election ID")
    void windowUpdateKeepsIdentity() {
        Election election = new Election("E1", "Test Election", START, END);
        Election updated = election.withWindow("Renamed Election", START.plusDays(1), END.plusDays(2));
        assertEquals("E1", updated.getElectionId());
        assertEquals("Renamed Election", updated.getElectionName());
    }
}
