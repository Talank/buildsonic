package smell.checker.maven

import org.junit.jupiter.api.Test
import smell.StateFlag

import static util.MavenUtil.MavenStrategy
import util.Util

import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class TestCheckerTest {
    @Test
    void checkParallelTest() {
        String repoPath = Paths.get(Util.codeDirectoryPath, "aintshy@hub").normalize().toString();
        POMChecker checker = new TestChecker(repoPath)
        assertEquals(StateFlag.CLOSE, checker.check(MavenStrategy.MAVEN_PARALLEL_TEST));

        repoPath = Paths.get(Util.codeDirectoryPath, "gaul@modernizer-maven-plugin").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.OPEN, checker.check(MavenStrategy.MAVEN_PARALLEL_TEST));

        repoPath = Paths.get(Util.codeDirectoryPath, "strongbox@strongbox").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_PARALLEL_TEST));
    }

    @Test
    void checkForkTest() {
        String repoPath = Paths.get(Util.codeDirectoryPath, "aintshy@hub").normalize().toString();
        POMChecker checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_FORK_TEST));

        repoPath = Paths.get(Util.codeDirectoryPath, "gaul@modernizer-maven-plugin").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_FORK_TEST));

        repoPath = Paths.get(Util.codeDirectoryPath, "strongbox@strongbox").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_FORK_TEST));

        repoPath = Paths.get(Util.codeDirectoryPath, "oshi@oshi").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.OPEN, checker.check(MavenStrategy.MAVEN_FORK_TEST));
    }

    @Test
    void checkReport() {
        String repoPath = Paths.get(Util.codeDirectoryPath, "aintshy@hub").normalize().toString();
        POMChecker checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_REPORT_GENERATION));

        repoPath = Paths.get(Util.codeDirectoryPath, "gaul@modernizer-maven-plugin").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_REPORT_GENERATION));

        repoPath = Paths.get(Util.codeDirectoryPath, "strongbox@strongbox").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_REPORT_GENERATION));

        repoPath = Paths.get(Util.codeDirectoryPath, "oshi@oshi").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_REPORT_GENERATION));

        repoPath = Paths.get(Util.codeDirectoryPath, "fake@fake").normalize().toString();
        checker = new TestChecker(repoPath)
        assertEquals(StateFlag.OPEN, checker.check(MavenStrategy.MAVEN_REPORT_GENERATION));

    }
}
