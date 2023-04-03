package smell.checker.maven

import org.junit.jupiter.api.Test
import smell.StateFlag
import util.MavenUtil
import util.Util

import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.assertEquals
import static util.MavenUtil.MavenStrategy


class CompilationCheckerTest {
    @Test
    void checkForkCompilation() {
        String repoPath = Paths.get(Util.codeDirectoryPath, "aintshy@hub").normalize().toString();
        POMChecker checker = new CompilationChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_COMPILER_DAEMON));

        repoPath = Paths.get(Util.codeDirectoryPath, "gaul@modernizer-maven-plugin").normalize().toString();
        checker = new CompilationChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_COMPILER_DAEMON));

        repoPath = Paths.get(Util.codeDirectoryPath, "strongbox@strongbox").normalize().toString();
        checker = new CompilationChecker(repoPath)
        assertEquals(StateFlag.DEFAULT, checker.check(MavenStrategy.MAVEN_COMPILER_DAEMON));

        repoPath = Paths.get(Util.codeDirectoryPath, "fake@fake").normalize().toString();
        checker = new CompilationChecker(repoPath)
        assertEquals(StateFlag.OPEN, checker.check(MavenStrategy.MAVEN_COMPILER_DAEMON));
    }
}
