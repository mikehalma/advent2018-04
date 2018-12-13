import java.time.LocalDateTime

data class LogEntry(val time: LocalDateTime, val action: String)

fun loadLogEntry(logEntryString: String): LogEntry {
    val regex = """\[(\d+)-(\d+)-(\d+) (\d+):(\d+)\] (.*)""".toRegex()
    val result = regex.find(logEntryString)
    val (year, month, day, hour, minute, action) = result!!.destructured
    return LogEntry(LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt()), action)
}

fun loadLogEntries(logs: List<String>): Set<LogEntry> {
    return logs.map {loadLogEntry(it)}.toSortedSet(compareBy {it.time})
}