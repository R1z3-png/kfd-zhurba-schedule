package schedule

import java.time.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class SessionTest {
    private val date = LocalDate.of(2026, 10, 12)

    @Test
    fun clearedNamesKeepsAssessment() {
        val names = mutableListOf("Ivanov", "Petrov")

        val assessment = Assessment(
            date = date,
            topic = "Topic 1",
            place = "Mephi",
            teachers = names,
        )
        names.clear() // После создания занятия изменение переданного списка не должно менять объект
        // У созданной сдачи по-прежнему должны остаться два преподавателя.

        assertEquals(listOf("Ivanov", "Petrov"), assessment.teachers)
    }

    @Test
    fun returnedTeachersListKeepsAssessment() {
        val assessment = Assessment(
            date = date,
            topic = "Topic 2",
            place = "Mgtu",
            teachers = listOf("Ivanov", "Petrov"),
        )

        val returned: List<String> = assessment.teachers

        assertEquals(listOf("Ivanov", "Petrov"), returned)
    }

    @Test
    fun blankTopicRejected() {
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "", place = "Mephi", teachers = listOf("Ivanov", "Petrov"))
        }
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "   ", place = "Mephi", teachers = listOf("Ivanov", "Petrov"))
        }
    }

    @Test
    fun yearOutOfRangeIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = LocalDate.of(0, 1, 1),
                topic = "Topic 3", place = "Mephi", teachers = listOf("Ivanov", "Petrov"),)
        }
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = LocalDate.of(10000, 1, 1),
                topic = "Topic 3", place = "Mephi", teachers = listOf("Ivanov", "Petrov"),)
        }
    }

    @Test
    fun emptyTeachersList() {
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "Topic 3", place = "Mephi", teachers = listOf())
        }
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "Topic 3", place = "Mephi", teachers = listOf("",""))
        }
    }

    @Test
    fun oneTeacherInAssessment() {
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "Topic 3", place = "Mephi", teachers = listOf("Ivanov"))
        }
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "Topic 3", place = "Mephi", teachers = listOf("Ivanov", ""))
        }
    }

    @Test
    fun twoSameTeachersInAssessment() {
        assertFailsWith<IllegalArgumentException> {
            Assessment(date = date, topic = "Topic 3", place = "Mephi", teachers = listOf("Ivanov", "Ivanov"))
        }
    }
}