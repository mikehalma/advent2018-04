import junit.framework.TestCase.assertEquals
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.StringContains.containsString
import org.junit.Test
import java.time.LocalDateTime

class DutyLogTest {

    @Test
    fun loadLogEntry_simple() {
        assertThat(loadLogEntry("[1518-11-01 00:00] Guard #10 begins shift"),
            `is`(LogEntry(LocalDateTime.of(1518, 11, 1, 0, 0),
            "Guard #10 begins shift")))
    }

    @Test
    fun loadLogEntries_order() {
        val logs = listOf(
            "[1518-11-01 00:01] Guard #2 begins shift",
            "[1518-11-01 01:00] Guard #4 begins shift",
            "[1518-11-01 00:00] Guard #1 begins shift",
            "[1519-11-01 00:00] Guard #8 begins shift",
            "[1518-11-01 00:10] Guard #3 begins shift",
            "[1518-11-01 10:00] Guard #5 begins shift",
            "[1518-11-02 00:00] Guard #6 begins shift",
            "[1518-12-01 00:00] Guard #7 begins shift"
        )
        val logEntries = loadLogEntries(logs)
        logEntries.forEachIndexed { index, logEntry ->
            assertThat(logEntry.action, containsString((index+1).toString()))
        }
    }

    @Test
    fun guardActionOf_correctEnum() {
        assertThat(GuardAction.of("[1519-11-01 00:00] Guard #8 begins shift"), `is`(GuardAction.BEGINS_SHIFT))
        assertThat(GuardAction.of("[1519-11-01 00:00] falls asleep"), `is`(GuardAction.FALLS_ASLEEP))
        assertThat(GuardAction.of("[1519-11-01 00:00] wakes up"), `is`(GuardAction.WAKES_UP))
    }

    @Test
    fun getGuard() {
        assertThat(getGuard("Guard #1 begins shift"), `is`(1))
        assertThat(getGuard("Guard #22 begins shift"), `is`(22))
        assertThat(getGuard("Guard #333333 begins shift"), `is`(333333))
    }

    @Test
    fun getGuardDuties_returnsGuards() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 01:00] Guard #4 begins shift"
            )

        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties.size, `is`(2))
        assertThat(guardDuties[0].guard, `is`(2))
        assertThat(guardDuties[1].guard, `is`(4))
    }

    @Test
    fun getGuardDuties_capturesTimes() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].time, `is`(LocalDateTime.of(1518, 11, 1, 0, 0)))
        assertThat(guardDuties[1].time, `is`(LocalDateTime.of(1518, 11, 1, 1, 0)))
    }

    @Test
    fun getGuardDuties_capturesAwake() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepFullHour() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:00] falls asleep",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`((0..59).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepNotFullHour() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`((1..59).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepWakesUp() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:59] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`((1..58).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepWakesUpTwice() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:02] wakes up",
            "[1518-11-01 00:58] falls asleep",
            "[1518-11-01 00:59] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf(1, 58)))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepWakesUpThrice() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:02] wakes up",
            "[1518-11-01 00:30] falls asleep",
            "[1518-11-01 00:33] wakes up",
            "[1518-11-01 00:58] falls asleep",
            "[1518-11-01 00:59] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf(1, 30, 31, 32, 58)))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))
    }

    @Test
    fun getAsleepMinutes_all() {
        val logEntry = LogEntry(LocalDateTime.of(1518, 11, 1, 0, 0), "falls asleep")
        assertEquals(getMinutesLeftInHour(logEntry), (0..59).toMutableList())
    }

    @Test
    fun getGuardDuties_twoGuards() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:02] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift",
            "[1518-11-01 00:30] falls asleep",
            "[1518-11-01 00:33] wakes up"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf(1, 30, 31, 32)))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))
    }

    @Test
    fun getGuardDuties_threeGuards() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:02] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift",
            "[1518-11-01 01:30] falls asleep",
            "[1518-11-01 01:33] wakes up",
            "[1518-11-02 00:00] Guard #3 begins shift",
            "[1518-11-02 00:55] falls asleep",
            "[1518-11-02 00:57] wakes up"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf(1)))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf(30, 31, 32)))
        assertThat(guardDuties[2].asleep, `is`(mutableListOf(55, 56)))
    }

    @Test
    fun getGuardDuties_fromFile() {
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(loadLogFile("guardDutiesSimple.txt")))

        assertThat(guardDuties[0].asleep, `is`((1..59).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDutySummary_simple() {
        // TODO we needa function that produces a summary for each guard with total minutes asleep and most common minutes asleep
        // start with single guard single sleep, then 2 single sleep, then 1 multi sleep, then 1 mutli days, then 2 multi days
    }

}