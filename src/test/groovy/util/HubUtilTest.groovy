package util

import org.junit.jupiter.api.Test

class HubUtilTest {
    @Test
    void testForkRepo() {
        String repoPath = GitUtilTest.repoPath
        HubUtil.forkRepo(repoPath)
    }
}
