package schedule
import java.time.LocalDate

private fun requireValidTopic(topic: String) {
    requireValidText(topic, "Topic")
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
        requireValidTopic(topic)
        requireValidPlace(place)
        requireValidTeachers(teachers, minimumCount = 1)
    }
}

data class Assessment(
    override val date: LocalDate,
    override val topic: String,
    val place: String,
    val teachers: List<String>,
) : Session {
    init {
        requireValidTopic(topic)
        requireValidPlace(place)
        requireValidTeachers(teachers, minimumCount = 2)
    }
}

data class Cancellation(
    override val date: LocalDate,
    override val topic: String,
) : Session {
    init {
        requireValidTopic(topic)
    }
}