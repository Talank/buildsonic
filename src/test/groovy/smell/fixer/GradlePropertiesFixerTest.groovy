package smell.fixer

import org.junit.jupiter.api.Test
import smell.fixer.Gradle.GradlePropertiesFixer

import java.nio.file.Paths

import static util.GradleUtil.*

class GradlePropertiesFixerTest {
    @Test
    void testModifyProperties() {
        String repoPath1 = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "repo1").normalize().toString()
        def list = [GradleStrategy.PARALLEL_BUILDS, GradleStrategy.CACHING]
        GradlePropertiesFixer.modifyProperties(repoPath1, list)
    }
}
