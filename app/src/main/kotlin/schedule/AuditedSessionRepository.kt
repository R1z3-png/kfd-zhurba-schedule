package schedule

data class AuditEvent(
    val operation: String,
    val count: Int,
)

class AuditedSessionRepository(
    private val delegate: SessionRepository,
) : SessionRepository by delegate {

    private val auditLog = mutableListOf<AuditEvent>()
    private var addedCount = 0

    val auditEvents: List<AuditEvent>
        get() = auditLog.toList()

    val totalAdded: Int
        get() = addedCount

    override fun add(session: Session) {
        delegate.add(session)
        auditLog.add(AuditEvent(operation = "add", count = 1))
        addedCount += 1
    }

    override fun addAll(newSessions: List<Session>) {
        delegate.addAll(newSessions)
        auditLog.add(AuditEvent(operation = "addAll", count = newSessions.size)) // addAll нескольких занятий создает 1 событие
        addedCount += newSessions.size
    }
}

