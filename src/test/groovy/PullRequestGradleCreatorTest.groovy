import org.junit.jupiter.api.Test
import static util.GradleUtil.GradleStrategy

import static org.junit.jupiter.api.Assertions.*;

class PullRequestGradleCreatorTest {

    @Test
    void testRun() {
    }

    @Test
    void testGetDescription() {
        List<GradleStrategy> strategies = new ArrayList<>()
        String originRepoName = ""
        def (title, description, outFilePaths) = PullRequestGradleCreator.getDescription(strategies,originRepoName)
        assertEquals("Improve GRADLE build Performance", title)
        assertEquals("\n\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n", description)
        assertEquals(0,outFilePaths.size())

        strategies.add(GradleStrategy.GRADLE_PARALLEL_TEST)
        (title, description, outFilePaths) = PullRequestGradleCreator.getDescription(strategies,originRepoName)
        assertEquals("Improve GRADLE build Performance", title)
        def expectedDescription = "# changes in file:build.gradle\n\n[Parallel test execution maxParallelForks](https://docs.gradle.org/current/userguide/performance.html#parallel_test_execution), running multiple test cases in parallel is useful and helpful when there are several CPU cores.\n"+
               "\n\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n"
        assertEquals(expectedDescription, description)
        assertEquals(1,outFilePaths.size())
        assertEquals(true,outFilePaths[0].endsWith("GRADLE_PARALLEL_TEST.txt"))

        strategies.add(GradleStrategy.FILE_SYSTEM_WATCHING)
        (title, description, outFilePaths) = PullRequestGradleCreator.getDescription(strategies,originRepoName)
        assertEquals("Improve GRADLE build Performance", title)
        expectedDescription =  "# changes in file:build.gradle\n" +
                "\n[Parallel test execution maxParallelForks](https://docs.gradle.org/current/userguide/performance.html#parallel_test_execution), running multiple test cases in parallel is useful and helpful when there are several CPU cores.\n" +
                "\n"+
                "# changes in file:gradle.properties\n" +
                "\n[file system watching](https://blog.gradle.org/introducing-file-system-watching),gradle version for this project is between 6.5 and 6.9, since Gradle 7.0 file system watching is enabled by default.\n" +
                "File system watching can significantly accelerate incremental builds and significantly reduce the amount of disk I/O needed\n" +
                "\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n"
        assertEquals(expectedDescription, description)
        assertEquals(2,outFilePaths.size())

    }

    @Test
    void testUseGradleStrategy() {
        /*
        Repository repository = null
        String repoPath = ""
        String originRepoName = ""
        GradleStrategy strategy = null;
        boolean flag = PullRequestGradleCreator.useGradleStrategy(repository,repoPath,originRepoName,strategy)
        assertEquals(false,flag)
        */
    }

    @Test
    void testGetAndApplyGradleStrategies() {
        /*
        Repository repository = null
        String repoPath = ""
        String originRepoName = ""
        List<GradleStrategy> strategies= PullRequestGradleCreator.getAndApplyGradleStrategies(repository,repoPath,originRepoName)
        assertEquals(0,strategies.size())
         */
    }

    @Test
    void testCreateGradlePullRequest() {
    }
}