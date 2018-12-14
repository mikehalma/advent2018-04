import java.time.LocalDateTime

data class LogEntry(val time: LocalDateTime, val action: String)

data class GuardDuty(val guard: Int, val time: LocalDateTime)

enum class GuardAction {
    BEGINS_SHIFT, WAKES_UP, FALLS_ASLEEP;

    companion object {
        fun of(action: String): GuardAction {
            return when {
                action.contains("begins shift") -> BEGINS_SHIFT
                action.contains("falls asleep") -> FALLS_ASLEEP
                action.contains("wakes up") -> WAKES_UP
                else -> throw IllegalArgumentException()
            }
        }
    }
}

fun loadLogEntry(logEntryString: String): LogEntry {
    val regex = """\[(\d+)-(\d+)-(\d+) (\d+):(\d+)] (.*)""".toRegex()
    val result = regex.find(logEntryString)
    val (year, month, day, hour, minute, action) = result!!.destructured
    return LogEntry(LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt()), action)
}

fun loadLogEntries(logs: List<String>): List<LogEntry> {
    return logs.map {loadLogEntry(it)}.toSortedSet(compareBy {it.time}).toList()
}

fun getGuardDuties(logEntries: List<LogEntry>): List<GuardDuty> {
    val guardDuties: MutableList<GuardDuty> = mutableListOf()
    for (logEntry in logEntries) {
        val guardAction = GuardAction.of(logEntry.action)
        when (guardAction) {
            GuardAction.BEGINS_SHIFT -> guardDuties.add(GuardDuty(getGuard(logEntry.action), logEntry.time))
            else -> throw IllegalArgumentException()
        }
    }
    return guardDuties
}

fun getGuard(action: String): Int {
    val regex = """.*#(\d+).*""".toRegex()
    val result = regex.find(action)
    val (guard) = result!!.destructured
    return guard.toInt()
}