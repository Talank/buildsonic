package smell

import org.junit.jupiter.api.Test
import smell.checker.gradle.GradleChecker

import static org.junit.Assert.*

import java.util.regex.Matcher
import java.util.regex.Pattern

import static util.GradleUtil.GradleStrategy

class GradleCheckerTest {
    @Test
    void testGroovyMap() {
        def map = [GRADLE_DAEMON: 0,
                                                (GradleStrategy.CACHING): 1]
        assertNull(map.get(GradleStrategy.GRADLE_DAEMON))
        assertEquals(0, map.get('GRADLE_DAEMON'))
        assertEquals(0, map['GRADLE_DAEMON'])
        assertEquals(1, map.get(GradleStrategy.CACHING))
        assertEquals(1, map[GradleStrategy.CACHING])
    }

    @Test
    void testRegularExpression() {
        //这个文件解析有问题
        String s = "https://services.gradle.org/distributions/gradle-4.10.2-all.zip"
        Pattern pattern = ~/gradle-([.\d]+)-\w+\.zip/
        Matcher matcher = pattern.matcher(s)
        println(matcher)
        println(matcher.matches())
        if (matcher.find()) {
            println(matcher.group(1))
        }
    }

    @Test
    void testCompareVersion() {
        assertEquals(0, GradleChecker.compareVersion("6.8.1", "6.8.1"))
        assertEquals(1, GradleChecker.compareVersion("6.8.1", "6.5"))
        assertEquals(-1, GradleChecker.compareVersion("6.8.1", "7.0"))
        assertEquals(1, GradleChecker.compareVersion(null, "6.5"))
    }
}
