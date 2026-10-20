package schedule
import java.time.LocalDate
import java.util.Collections

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
    // forEachIndexed дает индекс элемента и сам элемент
}

private fun requireValidText(value: String, field: String) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) {"$field must not have leading or trailing whitespace"}
    require(value.none { it.isISOControl() }) {"$field must not contain control characters"}
    // .none вернет true если ни для одного элемента предикат не вернул true
}

sealed interface Session {
    val date: LocalDate
    val topic: String
}

// copy() имеет ту же видимость что и основной конструктор
@ConsistentCopyVisibility
data class Lecture private constructor(
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
    companion object {
        operator fun invoke(
            date: LocalDate,
            topic: String,
            place: String,
            teachers: List<String>,
        ): Lecture = Lecture(date = date,
            topic = topic,
            place = place,
            teachers = Collections.unmodifiableList(teachers.toList()))
    }
}

@ConsistentCopyVisibility
data class Practice private constructor(
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
    companion object {
        operator fun invoke(
            date: LocalDate,
            topic: String,
            place: String,
            teachers: List<String>,
        ): Practice = Practice(date = date,
            topic = topic,
            place = place,
            teachers = Collections.unmodifiableList(teachers.toList()))
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
            teachers = Collections.unmodifiableList(teachers.toList()),
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