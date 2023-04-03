package smell.checker.maven;

import org.junit.jupiter.api.Test
import util.Util

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MultiModuleCheckerTest {

    @Test
    void isMultiModule() {
        String repoPath = Paths.get(Util.codeDirectoryPath, "miltonio@milton2").normalize().toString();
        assertTrue(MultiModuleChecker.isMultiModule(repoPath));
        repoPath = Paths.get(Util.codeDirectoryPath, "apache@commons-collections").normalize().toString();
        assertFalse(MultiModuleChecker.isMultiModule(repoPath));
    }
}