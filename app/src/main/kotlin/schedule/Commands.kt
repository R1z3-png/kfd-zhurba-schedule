package schedule

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

private data class ParsedSchedule(
    val sessions: List<Session>,
    val failures: List<SessionParseResult.Failure>,
    val totalLines: Int,
)

private fun parseScheduleFile(file: String): ParsedSchedule {
    val sessions = mutableListOf<Session>()
    val failures = mutableListOf<SessionParseResult.Failure>()
    var totalLines = 0

    forEachScheduleLine(file) { lineNumber, line ->
        totalLines += 1

        when (val result = parseSessionLine(lineNumber, line)) {
            is SessionParseResult.Success -> sessions.add(result.session)
            is SessionParseResult.Failure -> failures.add(result)
        }
    }

    return ParsedSchedule(sessions, failures, totalLines)
}

private fun printRejections(failures: List<SessionParseResult.Failure>) {
    for (failure in failures) {
        println(formatRejection(failure))
    }
}


fun runPeriod(command: Command.Period): Int {
    val schedule = parseScheduleFile(command.file)

    printRejections(schedule.failures)

    val selected = schedule.sessions.filter { session ->
        session.date in command.from..command.to
    }

    for (session in selected) {
        println(formatSession(session))
    }

    println("Total: ${selected.size}")

    return if (schedule.failures.isEmpty()) 0 else 1
}

fun runType(command: Command.ByType): Int {
    val schedule = parseScheduleFile(command.file)

    printRejections(schedule.failures)

    val selected = schedule.sessions.filter { session ->
        session.typeName() == command.type.name
    }

    for (session in selected) {
        println(formatSession(session, withTeachers = true))
    }

    println("Total: ${selected.size}")

    return if (schedule.failures.isEmpty()) 0 else 1
}

fun runErrors(command: Command.Errors): Int {
    val schedule = parseScheduleFile(command.file)

    printRejections(schedule.failures)

    println("Rejected: ${schedule.failures.size} of ${schedule.totalLines}")

    return if (schedule.failures.isEmpty()) 0 else 1
}

private fun Session.typeName(): String =
    when (this) {
        is Lecture -> SessionType.LECTURE.name
        is Practice -> SessionType.PRACTICE.name
        is Assessment -> SessionType.EXAM.name
        is Cancellation -> SessionType.CANCELLED.name
    }

private fun formatSession(
    session: Session,
    withTeachers: Boolean = false,
): String {
    val base = listOf(
        session.date.toString(),
        session.typeName(),
        session.topic,
    )

    return when (session) {
        is Cancellation ->
            (base + if (withTeachers) listOf("-", "-") else listOf("-"))
                .joinToString(" | ")

        is Lecture ->
            (base + listOf(session.place) + teachersPart(session.teachers, withTeachers))
                .joinToString(" | ")

        is Practice ->
            (base + listOf(session.place) + teachersPart(session.teachers, withTeachers))
                .joinToString(" | ")

        is Assessment ->
            (base + listOf(session.place) + teachersPart(session.teachers, withTeachers))
                .joinToString(" | ")

    }
}

private fun teachersPart(
    teachers: List<String>,
    withTeachers: Boolean,
): List<String> =
    if (withTeachers) listOf(teachers.joinToString(", ")) else emptyList()