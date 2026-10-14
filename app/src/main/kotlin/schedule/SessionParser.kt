package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface SessionParseResult {
    data class Success(
        val session: Session,
    ) : SessionParseResult

    data class Failure(
        val lineNumber: Long,
        val reason: SessionParseFailure,
    ) : SessionParseResult
}

enum class TextField {
    TOPIC,
    LOCATION,
    TEACHER,
}

enum class TextViolation {
    BLANK,
    OUTER_WHITESPACE,
    CONTROL_CHARACTER,
    MISSING_VALUE,
}

sealed interface SessionParseFailure {
    data object EmptyLine : SessionParseFailure

    data class WrongFieldCount(
        val actual: Int,
    ) : SessionParseFailure

    data class InvalidDateFormat(
        val value: String,
    ) : SessionParseFailure

    data class InvalidYear(
        val year: Int,
    ) : SessionParseFailure

    data class NonexistentDate(
        val value: String,
    ) : SessionParseFailure

    data class UnknownType(
        val value: String,
    ) : SessionParseFailure

    data class InvalidText(
        val field: TextField,
        val violation: TextViolation,
        val teacherIndex: Int? = null,
    ) : SessionParseFailure

    data class DuplicateTeacher(
        val name: String,
    ) : SessionParseFailure

    data class TooFewTeachers(
        val minimum: Int,
        val actual: Int,
    ) : SessionParseFailure

    data object CancelledLocationMustBeDash : SessionParseFailure
    data object CancelledTeachersMustBeDash : SessionParseFailure
}

private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

fun parseSessionLine(
    lineNumber: Long,
    line: String,
): SessionParseResult {
    fun reject(reason: SessionParseFailure): SessionParseResult.Failure =
        SessionParseResult.Failure(lineNumber, reason)

    if (line.isEmpty()) {
        return reject(SessionParseFailure.EmptyLine)
    }

    // split с символьным разделителем сохраняет конечные пустые поля.
    val fields = line.split('|')

    if (fields.size != 5) {
        return reject(
            SessionParseFailure.WrongFieldCount(actual = fields.size)
        )
    }

    val (dateText, typeText, topic, location, teachersText) = fields

    if (!datePattern.matches(dateText)) {
        return reject(
            SessionParseFailure.InvalidDateFormat(dateText)
        )
    }

    val year = dateText.substring(0, 4).toInt()

    if (year !in 1..9999) {
        return reject(SessionParseFailure.InvalidYear(year))
    }

    val date = try {
        LocalDate.parse(dateText)
    } catch (_: DateTimeParseException) {
        return reject(
            SessionParseFailure.NonexistentDate(dateText)
        )
    }

    val type = SessionType.entries.firstOrNull { it.name == typeText }
        ?: return reject(SessionParseFailure.UnknownType(typeText))

    findTextFailure(topic, TextField.TOPIC)?.let {
        return reject(it)
    }

    if (type == SessionType.CANCELLED) {
        if (location != "-") {
            return reject(
                SessionParseFailure.CancelledLocationMustBeDash
            )
        }

        if (teachersText != "-") {
            return reject(
                SessionParseFailure.CancelledTeachersMustBeDash
            )
        }

        return SessionParseResult.Success(
            Cancellation(
                date = date,
                topic = topic,
            )
        )
    }

    findTextFailure(
        value = location,
        field = TextField.LOCATION,
        forbidDash = true,
    )?.let {
        return reject(it)
    }

    val teachers = if (teachersText.isEmpty()) {
        emptyList()
    } else {
        teachersText.split(',')
    }

    for ((index, teacher) in teachers.withIndex()) {
        findTextFailure(
            value = teacher,
            field = TextField.TEACHER,
            forbidDash = true,
            teacherIndex = index + 1,
        )?.let {
            return reject(it)
        }
    }

    val seen = mutableSetOf<String>()

    for (teacher in teachers) {
        if (!seen.add(teacher)) {
            return reject(
                SessionParseFailure.DuplicateTeacher(teacher)
            )
        }
    }

    val minimum = if (type == SessionType.EXAM) 2 else 1

    if (teachers.size < minimum) {
        return reject(
            SessionParseFailure.TooFewTeachers(
                minimum = minimum,
                actual = teachers.size,
            )
        )
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

private fun findTextFailure(
    value: String,
    field: TextField,
    forbidDash: Boolean = false,
    teacherIndex: Int? = null,
): SessionParseFailure.InvalidText? {
    val violation = when {
        value.isBlank() -> TextViolation.BLANK
        value != value.trim() -> TextViolation.OUTER_WHITESPACE
        value.any { it.isISOControl() } -> TextViolation.CONTROL_CHARACTER
        forbidDash && value == "-" -> TextViolation.MISSING_VALUE
        else -> return null
    }

    return SessionParseFailure.InvalidText(
        field = field,
        violation = violation,
        teacherIndex = teacherIndex,
    )
}