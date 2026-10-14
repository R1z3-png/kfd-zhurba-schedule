package schedule

interface SessionRepository {
    fun add(session: Session)
    fun addAll(newSessions: List<Session>)
    fun all(): List<Session>
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session) {
        sessions.add(session)
    }

    override fun addAll(newSessions: List<Session>) {
        sessions.addAll(newSessions)
    }

    override fun all(): List<Session> = sessions.toList()
}