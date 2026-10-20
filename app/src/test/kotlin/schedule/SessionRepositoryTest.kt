package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SessionRepositoryTest {
    private val date = LocalDate.of(2026, 10, 12)

    private fun lecture(topic: String) = Lecture(
        date = date,
        topic = topic,
        place = "Mephi",
        teachers = listOf("Ivanov"),
    )

    @Test
    fun addStoresOneSession() {
        val repository = InMemorySessionRepository()
        val session = lecture("Physics")

        repository.add(session)

        assertEquals(listOf(session), repository.all())
    }

    @Test
    fun addAllPreservesSourceOrderAndDuplicates() {
        val repository = InMemorySessionRepository()
        val firstSession = lecture("Physics")
        val secondSession = lecture("VTA")

        repository.addAll(listOf(firstSession, secondSession, firstSession))

        assertEquals(listOf(firstSession, secondSession, firstSession), repository.all())
    }

    @Test
    fun addAllowsDuplicateSessions() {
        val repository = InMemorySessionRepository()
        val session = lecture("Physics")

        repository.add(session)
        repository.add(session)

        assertEquals(listOf(session, session), repository.all())
    }

    @Test
    fun allReturnsIndependentSnapshot() {
        val repository = InMemorySessionRepository()
        val session = lecture("Physics")

        repository.add(session)
        val snapshot = repository.all()

        repository.add(lecture("VTA"))

        assertEquals(listOf(session), snapshot)
        assertEquals(2, repository.all().size)
    }

    @Test
    fun snapshotDoesNotExposeMutableRepoState() {
        val repository = InMemorySessionRepository()
        val session = lecture("Physics")

        repository.add(session)
        val snapshot = repository.all()

        assertEquals(listOf(session), snapshot)
        assertFailsWith<UnsupportedOperationException> {
            (snapshot as MutableList<Session>).add(session)
        }
        assertEquals(listOf(session), repository.all())
    }
}