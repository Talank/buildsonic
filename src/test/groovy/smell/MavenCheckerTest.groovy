package smell

import model.Repository
import org.junit.jupiter.api.Test
import smell.checker.maven.MultiModuleChecker

class MavenCheckerTest {
    @Test
    void testIsMultiModule() {
        Repository repository = new Repository();
        repository.setRepoName("apache@commons-lang")
        MultiModuleChecker.isMultiModule(repository)
    }
}
