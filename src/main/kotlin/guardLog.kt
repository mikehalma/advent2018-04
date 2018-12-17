import java.io.File
import java.nio.charset.Charset
import java.time.LocalDateTime

data class LogEntry(val time: LocalDateTime, val action: String)

data class GuardDuty(val guard: Int, val time: LocalDateTime, val asleep: MutableList<Int>)

enum class GuardAction {
    BEGINS_SHIFT, WAKES_UP, FALLS_ASLEEP;

    companion object {
        fun of(action: String): GuardAction {
            return when {
                action.contains("begins shift") -> BEGINS_SHIFT
                action.contains("falls asleep") -> FALLS_ASLEEP
                action.contains("wakes up") -> WAKES_UP
                else -> throw IllegalArgumentException("The action $action is not recognised")
            }
        }
    }
}

data class GuardDutySummary(
    val guard: Int,
    val totalMinutesAsleep: Int,
    val minutesAsleepLongest: Int,
    val frequencyOfLongestMinuteAsleep: Int
)

fun loadLogEntry(logEntryString: String): LogEntry {
    val regex = """\[(\d+)-(\d+)-(\d+) (\d+):(\d+)] (.*)""".toRegex()
    val result = regex.find(logEntryString)
    val (year, month, day, hour, minute, action) = result!!.destructured
    return LogEntry(LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt()), action)
}

fun loadLogEntries(logs: List<String>): List<LogEntry> {
    return logs.map {loadLogEntry(it)}.toList().sortedWith(compareBy( {it.time}, {it.action}))
}

fun getGuardDuties(logEntries: List<LogEntry>): List<GuardDuty> {
    val guardDuties: MutableList<GuardDuty> = mutableListOf()
    var fellAsleep = LocalDateTime.MIN
    for (logEntry in logEntries) {
        val guardAction = GuardAction.of(logEntry.action)
        when (guardAction) {
            GuardAction.BEGINS_SHIFT -> guardDuties.add(GuardDuty(getGuard(logEntry.action), logEntry.time, mutableListOf()))
            GuardAction.FALLS_ASLEEP -> fellAsleep = logEntry.time
            GuardAction.WAKES_UP -> guardDuties.last().asleep.addAll(getAsleepMinutes(fellAsleep, logEntry.time))
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

fun getAsleepMinutes(fellAsleep: LocalDateTime, wokeUp: LocalDateTime):MutableList<Int> {
    var time = fellAsleep
    val minutes = mutableListOf<Int>()
    while (time < wokeUp) {
        minutes.add(time.minute)
        time = time.plusMinutes(1)
    }
    return minutes
}


fun loadLogFile(fileName :String) :List<String> {
    return File(object {}.javaClass.getResource(fileName).toURI()).readLines(Charset.defaultCharset())
}

fun getGuardDutySummaries(allGuardDuties: List<GuardDuty>): List<GuardDutySummary> {
    val summariesByGuard = allGuardDuties.groupBy { it.guard }
    return summariesByGuard.map {(guard, singleGuardDuties) -> getGuardDutySummary(guard, singleGuardDuties)}.toList()
}

fun getGuardDutySummary(guard: Int, guardDuties: List<GuardDuty>): GuardDutySummary {
    val minutesAsleep = guardDuties.flatMap { it.asleep }.size
    val minutesAsleepCounts = guardDuties.flatMap { it.asleep }.groupingBy { it }.eachCount()
    val maxMinutesAsleep = minutesAsleepCounts.maxBy { it.value }?.key
    val frequencyOfLongestMinuteAsleep = minutesAsleepCounts.maxBy { it.value }?.value
    return GuardDutySummary(guard, minutesAsleep, maxMinutesAsleep?:0, frequencyOfLongestMinuteAsleep?:0)
}

fun getSleepiestGuard(allGuardDutySummaries: List<GuardDutySummary>): Int {
    val sleepiestGuard = allGuardDutySummaries.maxBy {it.totalMinutesAsleep}
    return if (sleepiestGuard != null) sleepiestGuard.guard * sleepiestGuard.minutesAsleepLongest else 0
}

fun getSleepiestGuard(fileName: String): Int {
    val logEntriesRaw = loadLogFile(fileName)
    val logEntries = loadLogEntries(logEntriesRaw)
    val guardDuties = getGuardDuties(logEntries)
    val guardDutySummaries = getGuardDutySummaries(guardDuties)
    return getSleepiestGuard(guardDutySummaries)
}

fun getMostFrequentSleeper(fileName: String): Int {
    val logEntriesRaw = loadLogFile(fileName)
    val logEntries = loadLogEntries(logEntriesRaw)
    val guardDuties = getGuardDuties(logEntries)
    val guardDutySummaries = getGuardDutySummaries(guardDuties)
    return getMostFrequentSleeper(guardDutySummaries)
}

fun getMostFrequentSleeper(allGuardDutySummaries: List<GuardDutySummary>): Int {
    val mostFrequentSleeper = allGuardDutySummaries.maxBy {it.frequencyOfLongestMinuteAsleep}
    return if (mostFrequentSleeper != null) mostFrequentSleeper.guard * mostFrequentSleeper.minutesAsleepLongest else 0
}