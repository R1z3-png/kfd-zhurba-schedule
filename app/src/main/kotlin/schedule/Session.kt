package schedule
import java.time.LocalDate

private fun requireValidTopic(topic: String) {
    requireValidText(topic, "Topic")
}

private fun requireValidDate(date: LocalDate) {
    require(date.year in 1..9999) {"Year must be in range 1..9999"}
}

private fun requireValidPlace(place: String) {
    requireValidText(place, "Place")
    require(place != "-") { "Place must not be '-'" }
}

private fun requireValidTeachers(teachers: List<String>, minimumCount: Int) {
    require(teachers.size >= minimumCount) {"At least $minimumCount teacher(s) are required"}
    require(teachers.distinct().size == teachers.size) {"Teacher names must be distinct"}
    teachers.forEachIndexed { index, teacher ->
        requireValidText(teacher, "Teacher name at index $index")
        require(teacher != "-") {"Teacher name at index $index must not be '-'"}
    }
}

private fun requireValidText(value: String, field: String) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) {"$field must not have leading or trailing whitespace"}
    require(value.none { it.isISOControl() }) {"$field must not contain control characters"}
}

interface Session {
    val date: LocalDate
    val topic: String
}

data class Lecture(
    override val date: LocalDate,
    override val topic: String,
    val place: String,
    val teachers: List<String>,
) : Session {
    init {
        requireValidDate(date)
        requireValidTopic(topic)
        requireValidPlace(place)
        requireValidTeachers(teachers, minimumCount = 1)
    }
}

data class Practice(
    override val date: LocalDate,
    override val topic: String,
    val place: String,
    val teachers: List<String>,
) : Session {
    init {
        requireValidDate(date)
        requireValidTopic(topic)
        requireValidPlace(place)
        requireValidTeachers(teachers, minimumCount = 1)
    }
}

@ConsistentCopyVisibility
data class Assessment private constructor(
    override val date: LocalDate,
    override val topic: String,
    val place: String,
    val teachers: List<String>,
) : Session {
    init {
        requireValidDate(date)
        requireValidTopic(topic)
        requireValidPlace(place)
        requireValidTeachers(teachers, minimumCount = 2)
    }

    companion object {
        operator fun invoke(
            date: LocalDate,
            topic: String,
            place: String,
            teachers: Collection<String>,
        ): Assessment = Assessment(
            date = date,
            topic = topic,
            place = place,
            teachers = teachers.toList(),
        )
    }
}

data class Cancellation(
    override val date: LocalDate,
    override val topic: String,
) : Session {
    init {
        requireValidDate(date)
        requireValidTopic(topic)
    }
}