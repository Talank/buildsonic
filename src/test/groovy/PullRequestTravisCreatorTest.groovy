import model.Repository
import org.junit.jupiter.api.Test
import util.GithubUtilTets
import util.Util
import util.TravisUtil

import java.nio.file.Paths

import static org.junit.Assert.*

class PullRequestTravisCreatorTest {
    @Test
    void testRun() {
        String originRepoName = GithubUtilTets.getFullRepoName()
        Repository repository = new Repository()
        repository.repoName = originRepoName
        repository.buildTool = 2
        //PullRequestCreator.run(repository, PullRequestCreator.StrategyCombine.TRAVIS, null)
        if (null)
            println("fdf=a=fw=f")
        println("===========")
    }

    @Test
    void testGetTravisStrategies() {
        String repoName = "apache/tinkerpop"
        String repoPath = Paths.get(Util.codeDirectoryPath, repoName.replace("/", "@")).toString()
        List<TravisUtil.TravisStrategy> strategies = PullRequestTravisCreator.getTravisStrategies(repoPath, repoName)
        assertTrue(strategies.contains(PullRequestTravisCreator.Strategy.TRAVIS_WAIT))
        assertEquals(1, strategies.size())

        repoName = "apereojava-cas-client"
        repoPath = Paths.get(Util.codeDirectoryPath, repoName.replace("/", "@")).toString()
        strategies = PullRequestTravisCreator.getTravisStrategies(repoPath, repoName)
        assertTrue(strategies.contains(PullRequestTravisCreator.Strategy.TRAVIS_CACHE))
        assertEquals(1, strategies.size())

        repoName = "apache@commons-collections"
        repoPath = Paths.get(Util.codeDirectoryPath, repoName.replace("/", "@")).toString()
        strategies = PullRequestTravisCreator.getTravisStrategies(repoPath, repoName)
        println(strategies)
        assertTrue(strategies.contains(PullRequestTravisCreator.Strategy.TRAVIS_CACHE))
        assertTrue(strategies.contains(PullRequestTravisCreator.Strategy.TRAVIS_FAST_FINISH))
        assertEquals(2, strategies.size())
    }
}
