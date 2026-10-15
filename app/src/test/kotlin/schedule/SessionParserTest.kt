package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionParserTest {
    private val date = LocalDate.of(2026, 10, 12)

    @Test
    fun invalidDateHasPriorityOverUnknownType() {
        val result = parseSessionLine(
            lineNumber = 1L,
            line = "2026-13-45|SEMINAR|Topic|Room 1|Ivanov",
        )

        val failure = assertIs<SessionParseResult.Failure>(result)

        assertEquals(
            SessionParseFailure.InvalidDate("2026-13-45"),
            failure.reason,
        )
        assertEquals(
            "line 1: invalid date \"2026-13-45\"",
            formatRejection(failure),
        )
    }

    @Test
    fun duplicateTeacherHasPriorityOverLaterChecks() {
        val result = parseSessionLine(
            lineNumber = 2L,
            line = "2026-10-05|EXAM|Topic|Room 1|Ivanov,Ivanov",
        )

        val failure = assertIs<SessionParseResult.Failure>(result)

        assertEquals(
            SessionParseFailure.DuplicateTeacher("Ivanov"),
            failure.reason,
        )
        assertEquals(
            "line 2: duplicate teacher \"Ivanov\"",
            formatRejection(failure),
        )
    }

    @Test
    fun blankFieldHasPriorityOverInvalidDate() {
        val result = parseSessionLine(
            lineNumber = 3L,
            line = "|LECTURE|Topic|Room 1|Ivanov",
        )

        val failure = assertIs<SessionParseResult.Failure>(result)

        assertEquals(
            SessionParseFailure.BlankField(TextField.DATE),
            failure.reason,
        )
        assertEquals(
            "line 3: empty required field \"date\"",
            formatRejection(failure),
        )
    }
}