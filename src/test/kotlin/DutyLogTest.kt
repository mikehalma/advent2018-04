import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.hamcrest.core.StringContains.containsString
import org.junit.Test
import java.time.LocalDateTime

class DutyLogTest {

    @Test
    fun loadLogEntry_simple() {
        assertThat(loadLogEntry("[1518-11-01 00:00] Guard #10 begins shift"),
            Is.`is`(LogEntry(LocalDateTime.of(1518, 11, 1, 0, 0),
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
}