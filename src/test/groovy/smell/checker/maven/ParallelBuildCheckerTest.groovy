package smell.checker.maven
import org.junit.jupiter.api.Test
import smell.StateFlag
import util.Util

import java.nio.file.Paths
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class ParallelBuildCheckerTest {
    @Test
    void testPattern() {
        String s = "mvn -Psafer -Pintegration -B -e -T 1C -Dcheckstyle.consoleOutput=false --update-snapshots verify"
        Matcher matcher = ParallelBuildChecker.pattern.matcher(s)
        matcher.find()
        assertEquals("1C", matcher.group(1))
        s = "HOME/mvn -V clean install -Dintegration.tests -T    2 -Daether.connector.resumeDownloads=false --log-file=build.log"
        matcher = ParallelBuildChecker.pattern.matcher(s)
        matcher.find()
        assertEquals("2", matcher.group(1))
        s = "mvn checkstyle:check -B -T 2.0C"
        matcher = ParallelBuildChecker.pattern.matcher(s)
        matcher.find()
        assertEquals("2.0C", matcher.group(1))
    }
    @Test
    void testParallelExecutionChecker() {
        String path = Paths.get(Util.codeDirectoryPath, "miltonio@milton2").normalize().toString();
        String repoName = "miltonio/milton2"
        assertEquals(StateFlag.DEFAULT, ParallelBuildChecker.parallelExecutionChecker(path, repoName))
        path = Paths.get(Util.codeDirectoryPath, "strongbox@strongbox").normalize().toString();
        repoName = "strongbox/strongbox"
        assertEquals(StateFlag.OPEN, ParallelBuildChecker.parallelExecutionChecker(path, repoName))
    }
}
