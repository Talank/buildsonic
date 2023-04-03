package util

import org.junit.jupiter.api.Test
import smell.TravisFixer

import java.nio.file.Paths

class GitUtilTest {
    static String fullRepoName ="YunLemon/travisLogAnalysis"
    static String repoPath = Paths.get(Util.forkDirectoryPath, fullRepoName.replace("/", "@")).toString()
    @Test
    void testCloneRepo() {
        GitUtil.cloneRepo(Util.forkDirectoryPath, fullRepoName)
    }

    @Test
    void testCreateAndCheckoutBranch() {
        GitUtil.createAndCheckoutBranch(repoPath, "feature")
    }

    @Test
    void testAddAndCommit() {
        String buildConfigFileName = Paths.get(repoPath, "pom.xml")
        String travisConfigFilePath = Paths.get(repoPath, ".travis.yml")
        TravisFixer.addCache(buildConfigFileName, travisConfigFilePath)
        GitUtil.addAndCommit(repoPath, "add cache")
    }

    @Test
    void testPush() {
        GitUtil.push(repoPath, "feature")
    }

    @Test
    void testUpstream() {
        GitUtil.setUpstream(repoPath, "Chen/travisLogAnalysis")
    }
}
