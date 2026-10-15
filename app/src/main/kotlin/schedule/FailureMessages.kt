package schedule

private fun escapeValue(value: String): String =
    buildString {
        for (char in value) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                else ->
                    if (char.code < 0x20 ||
                        char.code > 0x7E
                    ) {
                        append("\\u")
                        append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        append(char)
                    }
            }
        }
    }

fun formatFailureReason(reason: SessionParseFailure): String =
    when (reason) {
        SessionParseFailure.NoFieldSeparator ->
            "invalid field separator"

        is SessionParseFailure.WrongFieldCount ->
            "got ${reason.actual} fields, expected 5"

        is SessionParseFailure.BlankField ->
            "empty required field \"${fieldName(reason.field)}\""

        is SessionParseFailure.InvalidDate ->
            "invalid date \"${escapeValue(reason.value)}\""

        is SessionParseFailure.UnknownType ->
            "unknown session type \"${escapeValue(reason.value)}\""

        is SessionParseFailure.TooFewDistinctTeachers ->
            "type \"EXAM\" requires at least ${reason.minimum} teachers, " +
                    "got ${reason.actual}"

        is SessionParseFailure.ControlCharacter ->
            "${fieldName(reason.field)} must not contain control characters"

        is SessionParseFailure.OuterWhitespace ->
            "${fieldName(reason.field)} must not have leading or trailing whitespace"

        SessionParseFailure.CancelledLocationMustBeDash ->
            "type \"CANCELLED\" requires location \"-\""

        SessionParseFailure.CancelledTeachersMustBeDash ->
            "type \"CANCELLED\" requires teachers \"-\""

        SessionParseFailure.ActiveLocationMustNotBeDash ->
            "location must not be \"-\""

        is SessionParseFailure.InvalidTeacher ->
            teacherMessage(reason)

        is SessionParseFailure.DuplicateTeacher ->
            "duplicate teacher \"${escapeValue(reason.name)}\""
    }


fun formatRejection(failure: SessionParseResult.Failure): String =
    "line ${failure.lineNumber}: ${formatFailureReason(failure.reason)}"

private fun fieldName(field: TextField): String =
    when (field) {
        TextField.DATE -> "date"
        TextField.TYPE -> "type"
        TextField.TOPIC -> "topic"
        TextField.LOCATION -> "location"
        TextField.TEACHERS -> "teachers"
    }

private fun controlCharacterMessage(
    reason: SessionParseFailure.ControlCharacter,
): String {
    val field = fieldName(reason.field)
    val label = teacherLabel(reason.teacherIndex, field)

    return "$label must not contain control characters"
}

private fun outerWhitespaceMessage(
    reason: SessionParseFailure.OuterWhitespace,
): String {
    val field = fieldName(reason.field)
    val label = teacherLabel(reason.teacherIndex, field)

    return "$label must not have leading or trailing whitespace"
}

private fun teacherMessage(
    reason: SessionParseFailure.InvalidTeacher,
): String {
    val label = "teacher #${reason.teacherIndex}"

    return when (reason.reason) {
        TeacherViolation.BLANK ->
            "$label must not be blank"

        TeacherViolation.OUTER_WHITESPACE ->
            "$label must not have leading or trailing whitespace"

        TeacherViolation.CONTROL_CHARACTER ->
            "$label must not contain control characters"

        TeacherViolation.DASH ->
            "$label must not be \"-\""
    }
}

private fun teacherLabel(
    teacherIndex: Int?,
    field: String,
): String =
    if (teacherIndex == null) field else "teacher #$teacherIndex"