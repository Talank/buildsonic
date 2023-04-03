package smell.checker

import org.junit.jupiter.api.Test
import smell.StateFlag
import smell.checker.gradle.BuildGradleChecker

import java.nio.file.Paths

import static org.junit.Assert.*
import static util.GradleUtil.*

class BuildGradleCheckerTest {
    @Test
    void testCheckOpen() {
        String repoPath1 = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "repo1").normalize().toString()
        def list = strategiesOfCategory.get(GradleCategory.COMPILATION) + strategiesOfCategory.get(GradleCategory.TEST)
        for(GradleStrategy strategy : list) {
            StateFlag flag = BuildGradleChecker.check(repoPath1, strategy)
            assertEquals(StateFlag.OPEN, flag)
        }
    }

    @Test
    void testCheckDefault() {
        String repoPath2 = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "repo2").normalize().toString()
        def list = strategiesOfCategory.get(GradleCategory.COMPILATION) + strategiesOfCategory.get(GradleCategory.TEST)
        for(GradleStrategy strategy : list) {
            StateFlag flag = BuildGradleChecker.check(repoPath2, strategy)
            assertEquals(StateFlag.DEFAULT, flag)
        }
    }

    @Test
    void testCheckClose() {
        String repoPath3 = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "repo3").normalize().toString()
        def list = [GradleStrategy.GRADLE_COMPILER_DAEMON, GradleStrategy.GRADLE_INCREMENTAL_COMPILATION]
        for(GradleStrategy strategy : list) {
            StateFlag flag = BuildGradleChecker.check(repoPath3, strategy)
            assertEquals(StateFlag.CLOSE, flag)
        }
    }
}
