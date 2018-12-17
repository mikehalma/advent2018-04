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
            "[1518-11-01 00:59] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`((0..58).toList()))
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
    fun getGuardDuties_capturesAsleepWakesUpNotFullHour() {
        val logs = listOf(
            "[1518-11-01 00:02] Guard #2 begins shift",
            "[1518-11-01 00:03] falls asleep",
            "[1518-11-01 00:59] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`((3..58).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDuties_capturesAsleepWakesUpOverMidnight() {
        val logs = listOf(
            "[1518-11-01 23:58] Guard #2 begins shift",
            "[1518-11-01 23:59] falls asleep",
            "[1518-11-02 00:59] wakes up",
            "[1518-11-02 01:00] Guard #4 begins shift"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        val guard2minutes = mutableListOf(59)
        guard2minutes.addAll((0..58).toList())
        assertEquals(guardDuties[0].asleep, guard2minutes)
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
    fun getAsleepMinutes_overMidnight() {
        val expectedAsleepMinutes = mutableListOf(58, 59, 0, 1, 2)
        val actualAsleepMinutes = getAsleepMinutes(
            LocalDateTime.of(1518, 11, 1, 23, 58),
            LocalDateTime.of(1518, 11, 2, 0, 3)
        )
        assertThat(actualAsleepMinutes, `is`(expectedAsleepMinutes))
    }

    @Test
    fun getGuardDuties_twoGuards() {
        val logs = listOf(
            "[1518-11-01 00:00] Guard #2 begins shift",
            "[1518-11-01 00:01] falls asleep",
            "[1518-11-01 00:02] wakes up",
            "[1518-11-01 01:00] Guard #4 begins shift",
            "[1518-11-01 01:30] falls asleep",
            "[1518-11-01 01:33] wakes up"
        )
        val guardDuties: List<GuardDuty> = getGuardDuties(loadLogEntries(logs))

        assertThat(guardDuties[0].asleep, `is`(mutableListOf(1)))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf(30, 31, 32)))
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

        assertThat(guardDuties[0].asleep, `is`((1..58).toList()))
        assertThat(guardDuties[1].asleep, `is`(mutableListOf()))

    }

    @Test
    fun getGuardDutySummary_simple() {
        val guardDuty = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 0, 0), (1..58).toMutableList())
        val expected = listOf(GuardDutySummary(2, 58, 1, 1))

        val guardSummaries = getGuardDutySummaries(listOf(guardDuty))

        assertThat(guardSummaries, `is`(expected))
    }

    @Test
    fun getGuardDutySummary_twoDuties() {
        val guardDuty1 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 0, 0), (1..58).toMutableList())
        val guardDuty2 = GuardDuty(2, LocalDateTime.of(1518, 11, 2, 0, 0), (2..2).toMutableList())
        val expected = listOf(GuardDutySummary(2, 59, 2, 2))

        val guardSummaries = getGuardDutySummaries(listOf(guardDuty1, guardDuty2))

        assertThat(guardSummaries, `is`(expected))
    }

    @Test
    fun getGuardDutySummary_twoGuards() {
        val guardDuty1 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 0, 0), (10..11).toMutableList())
        val guardDuty2 = GuardDuty(4, LocalDateTime.of(1518, 11, 2, 0, 0), (15..15).toMutableList())
        val expected1 = GuardDutySummary(2, 2, 10, 1)
        val expected2 = GuardDutySummary(4, 1, 15, 1)
        val expected = listOf(expected1, expected2)

        val guardSummaries = getGuardDutySummaries(listOf(guardDuty1, guardDuty2))

        assertThat(guardSummaries, `is`(expected))
    }

    @Test
    fun getGuardDutySummary_twoGuardsTwoDuties() {
        val guardDuty1 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 0, 0), (10..13).toMutableList())
        val guardDuty2 = GuardDuty(4, LocalDateTime.of(1518, 11, 2, 0, 0), mutableListOf(15, 21))
        val guardDuty3 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 4, 0), (11..16).toMutableList())
        val guardDuty4 = GuardDuty(4, LocalDateTime.of(1518, 11, 2, 5, 0), (15..23).toMutableList())
        val expected1 = GuardDutySummary(2, 10, 11, 2)
        val expected2 = GuardDutySummary(4, 11, 15, 2)
        val expected = listOf(expected1, expected2)

        val guardSummaries = getGuardDutySummaries(listOf(guardDuty1, guardDuty2, guardDuty3, guardDuty4))

        assertThat(guardSummaries, `is`(expected))
    }

    @Test
    fun sleepiestGuard_simple() {
        val guardDuty1 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 0, 0), (10..13).toMutableList())
        val guardDuty2 = GuardDuty(4, LocalDateTime.of(1518, 11, 2, 0, 0), mutableListOf(15))
        val guardDuty3 = GuardDuty(2, LocalDateTime.of(1518, 11, 1, 4, 0), (13..16).toMutableList())
        val guardDuty4 = GuardDuty(4, LocalDateTime.of(1518, 11, 2, 5, 0), (15..33).toMutableList())

        val guardSummaries = getGuardDutySummaries(listOf(guardDuty1, guardDuty2, guardDuty3, guardDuty4))

        assertThat(getSleepiestGuard(guardSummaries), `is`(4 * 15))
    }

    @Test
    fun sleepiestGuard_example() {
        assertThat(getSleepiestGuard("guardDutiesExample.txt"), `is`(240))
    }

    @Test
    fun sleepiestGuard() {
        assertThat(getSleepiestGuard("guardDuties.txt"), `is`(50558))
    }

    @Test
    fun mostFrequentSleeper_example() {
        assertThat(getMostFrequentSleeper("guardDutiesExample.txt"), `is`(4455))
    }

    @Test
    fun mostFrequentSleeper() {
        assertThat(getMostFrequentSleeper("guardDuties.txt"), `is`(28198))
    }

}