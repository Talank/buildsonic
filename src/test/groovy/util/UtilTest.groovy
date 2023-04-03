package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void splitFullRepoName() {
        String fullRepoName ="YunLemon/travisLogAnalysis"
        def (userName, repoName) = Util.splitFullRepoName(fullRepoName)
        assertEquals("YunLemon", userName)
        assertEquals("travisLogAnalysis", repoName)
    }

    @Test
    void testGetShellFilePaths() {
        String path = System.getProperty("user.dir")
        println(path)
        Util.getShellFilePaths(path)
    }
}