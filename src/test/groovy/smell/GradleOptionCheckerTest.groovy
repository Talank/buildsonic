package smell

import model.Repository;
import org.junit.jupiter.api.Test
import util.MysqlUtil

import java.util.regex.Matcher;

import static org.junit.Assert.*;

class GradleOptionCheckerTest {

    @Test
    void testParallelExecutionChecker() {
        Matcher matcher = ('org.gradle.parallel = true' =~ /org.gradle.parallel\s*=\s*true/)
        assertEquals(1, matcher.size())
        if (matcher) {

        } else {
            fail()
        }
    }
}