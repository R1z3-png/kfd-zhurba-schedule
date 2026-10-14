package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditedSessionRepositoryTest {
    private val date = LocalDate.of(2026, 10, 12)

    private fun lecture(topic: String) = Lecture(
        date = date,
        topic = topic,
        place = "Mephi",
        teachers = listOf("Ivanov"),
    )

    @Test
    fun addCreatesOneEventWithCountOne() {
        val repository = AuditedSessionRepository(InMemorySessionRepository())
        val session = lecture("Physics")

        repository.add(session)

        assertEquals(listOf(session), repository.all())
        assertEquals(listOf(AuditEvent("add", 1)), repository.auditEvents)
        assertEquals(1, repository.totalAdded)
    }

    @Test
    fun addAllCreatesOneEventWithSourceSize() {
        val repository = AuditedSessionRepository(InMemorySessionRepository())
        val sessions = listOf(
            lecture("Physics"),
            lecture("VTA"),
            lecture("Algebra"),
        )

        repository.addAll(sessions)

        assertEquals(sessions, repository.all())
        assertEquals(listOf(AuditEvent("addAll", 3)), repository.auditEvents)
        assertEquals(3, repository.totalAdded)
    }

    @Test
    fun EmptyAddAllCreatesZeroCountEvent() {
        val repository = AuditedSessionRepository(InMemorySessionRepository())

        repository.addAll(emptyList())

        assertEquals(emptyList(), repository.all())
        assertEquals(listOf(AuditEvent("addAll", 0)), repository.auditEvents)
        assertEquals(0, repository.totalAdded)
    }

    @Test
    fun allDoesNotCreateAuditEvents() {
        val repository = AuditedSessionRepository(InMemorySessionRepository())
        val session = lecture("Physics")

        repository.add(session)
        repository.all()

        assertEquals(listOf(AuditEvent("add", 1)), repository.auditEvents)
    }
}