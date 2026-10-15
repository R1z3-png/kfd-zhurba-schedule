package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface SessionParseResult {
    data class Success(val session: Session) : SessionParseResult

    data class Failure(val lineNumber: Long, val reason: SessionParseFailure) : SessionParseResult
}

sealed interface SessionParseFailure {
    data object NoFieldSeparator : SessionParseFailure
    data object CancelledLocationMustBeDash : SessionParseFailure
    data object CancelledTeachersMustBeDash : SessionParseFailure
    data object ActiveLocationMustNotBeDash : SessionParseFailure

    data class WrongFieldCount(val actual: Int) : SessionParseFailure
    data class BlankField(val field: TextField) : SessionParseFailure
    data class InvalidDate(val value: String) : SessionParseFailure


    data class UnknownType(val value: String) : SessionParseFailure
    data class TooFewDistinctTeachers(
        val minimum: Int,
        val actual: Int,
    ) : SessionParseFailure
    data class ControlCharacter(val field: TextField, val teacherIndex: Int? = null) :
        SessionParseFailure
    data class OuterWhitespace(val field: TextField, val teacherIndex: Int? = null) :
        SessionParseFailure
    data class InvalidTeacher(val teacherIndex: Int, val reason: TeacherViolation) :
        SessionParseFailure
    data class DuplicateTeacher(val name: String) : SessionParseFailure
}

enum class TextField {
    DATE,
    TYPE,
    TOPIC,
    LOCATION,
    TEACHERS,
}

enum class TeacherViolation {
    BLANK,
    OUTER_WHITESPACE,
    CONTROL_CHARACTER,
    DASH,
}

private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

fun parseSessionLine(lineNumber: Long, line: String): SessionParseResult {
    fun fail(reason: SessionParseFailure) =
        SessionParseResult.Failure(lineNumber, reason)
    if ('|' !in line) {
        return fail(SessionParseFailure.NoFieldSeparator)
    }

    val fields = line.split('|')
    if (fields.size != 5) {
        return fail(SessionParseFailure.WrongFieldCount(fields.size))
    }

    val (dateText, typeText, topic, location, teachersText) = fields
    val blankField = sequenceOf(TextField.DATE to dateText,
        TextField.TYPE to typeText,
        TextField.TOPIC to topic,
        TextField.LOCATION to location,
        TextField.TEACHERS to teachersText,
    ).firstOrNull { (_, value) -> value.isBlank() }

    if (blankField != null) {
        return fail(SessionParseFailure.BlankField(blankField.first))
    }

    if (!datePattern.matches(dateText)) {
        return fail(SessionParseFailure.InvalidDate(dateText))
    }

    val year = dateText.substring(0, 4).toInt()
    if (year !in 1..9999) {
        return fail(SessionParseFailure.InvalidDate(dateText))
    }

    val date = try {
        LocalDate.parse(dateText)
    } catch (_: DateTimeParseException) {
        return fail(SessionParseFailure.InvalidDate(dateText))
    }


    val type = SessionType.entries.firstOrNull { it.name == typeText }
        ?: return fail(SessionParseFailure.UnknownType(typeText))

    val teachers = teachersText.split(',')

    if (type == SessionType.EXAM) {
        val validTeachers = teachers.filter { teacher ->
            teacher.isNotBlank() &&
                    teacher.trim() == teacher &&
                    teacher.none { it.isISOControl() } &&
                    teacher != "-"
        }

        if (validTeachers.size < 2) {
            return fail(
                SessionParseFailure.TooFewDistinctTeachers(
                    minimum = 2,
                    actual = validTeachers.size,
                )
            )
        }
    }


    findControlCharacterFailure(
        date = dateText,
        type = typeText,
        topic = topic,
        location = location,
        teachers = teachers,
    )?.let {
        return fail(it)
    }

    findOuterWhitespaceFailure(
        date = dateText,
        type = typeText,
        topic = topic,
        location = location,
        teachers = teachers,
    )?.let {
        return fail(it)
    }


    if (type == SessionType.CANCELLED) {
        if (location != "-") {
            return fail(SessionParseFailure.CancelledLocationMustBeDash)
        }

        if (teachersText != "-") {
            return fail(SessionParseFailure.CancelledTeachersMustBeDash)
        }

        return SessionParseResult.Success(
            Cancellation(
                date = date,
                topic = topic,
            )
        )
    }

    if (location == "-") {
        return fail(SessionParseFailure.ActiveLocationMustNotBeDash)
    }

    for ((index, teacher) in teachers.withIndex()) {
        val violation = teacherViolation(teacher)
        if (violation != null) {
            return fail(
                SessionParseFailure.InvalidTeacher(
                    teacherIndex = index + 1,
                    reason = violation,
                )
            )
        }
    }

    val seen = mutableSetOf<String>()
    for (teacher in teachers) {
        if (!seen.add(teacher)) {
            return fail(SessionParseFailure.DuplicateTeacher(teacher))
        }
    }

    val session = when (type) {
        SessionType.LECTURE -> Lecture(
            date = date,
            topic = topic,
            place = location,
            teachers = teachers,
        )

        SessionType.PRACTICE -> Practice(
            date = date,
            topic = topic,
            place = location,
            teachers = teachers,
        )

        SessionType.EXAM -> Assessment(
            date = date,
            topic = topic,
            place = location,
            teachers = teachers,
        )

        SessionType.CANCELLED -> error(
            "Cancelled session must be handled before active sessions"
        )
    }

    return SessionParseResult.Success(session)
}

private fun findControlCharacterFailure(
    date: String,
    type: String,
    topic: String,
    location: String,
    teachers: List<String>,
): SessionParseFailure? {
    if (date.any { it.isISOControl() }) {
        return SessionParseFailure.ControlCharacter(TextField.DATE)
    }
    if (type.any { it.isISOControl() }) {
        return SessionParseFailure.ControlCharacter(TextField.TYPE)
    }
    if (topic.any { it.isISOControl() }) {
        return SessionParseFailure.ControlCharacter(TextField.TOPIC)
    }
    if (location.any { it.isISOControl() }) {
        return SessionParseFailure.ControlCharacter(TextField.LOCATION)
    }

    teachers.forEachIndexed { index, teacher ->
        if (teacher.any { it.isISOControl() }) {
            return SessionParseFailure.ControlCharacter(
                field = TextField.TEACHERS,
                teacherIndex = index + 1,
            )
        }
    }

    return null
}

private fun findOuterWhitespaceFailure(
    date: String,
    type: String,
    topic: String,
    location: String,
    teachers: List<String>,
): SessionParseFailure? {
    if (date != date.trim()) {
        return SessionParseFailure.OuterWhitespace(TextField.DATE)
    }
    if (type != type.trim()) {
        return SessionParseFailure.OuterWhitespace(TextField.TYPE)
    }
    if (topic != topic.trim()) {
        return SessionParseFailure.OuterWhitespace(TextField.TOPIC)
    }
    if (location != location.trim()) {
        return SessionParseFailure.OuterWhitespace(TextField.LOCATION)
    }

    teachers.forEachIndexed { index, teacher ->
        if (teacher != teacher.trim()) {
            return SessionParseFailure.OuterWhitespace(
                field = TextField.TEACHERS,
                teacherIndex = index + 1,
            )
        }
    }

    return null
}

private fun teacherViolation(teacher: String): TeacherViolation? = when {
    teacher.isBlank() -> TeacherViolation.BLANK
    teacher != teacher.trim() -> TeacherViolation.OUTER_WHITESPACE
    teacher.any { it.isISOControl() } -> TeacherViolation.CONTROL_CHARACTER
    teacher == "-" -> TeacherViolation.DASH
    else -> null
}


