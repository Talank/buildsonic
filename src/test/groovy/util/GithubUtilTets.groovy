package util

import org.junit.jupiter.api.Test

import java.nio.file.Paths

class GithubUtilTets {
    static String fullRepoName ="ChenZhangg/CILink"
    static String repoPath = Paths.get(Util.forkDirectoryPath, fullRepoName.replace("/", "@")).toString()
    @Test
    void testFork() {
        GithubUtil.forkRepo(fullRepoName)
    }

    @Test
    void testDeleteRepo() {
        GithubUtil.deleteRepo("YunLemon/CILink")
    }

    @Test
    void testPullRequest() {
        //GithubUtil.pullRequest("ChenZhangg/CILink", "YunLemon:TRAVIS_CACHE_8", "title", "body")
        GithubUtil.pullRequest("webauthn4j/keycloak-webauthn-authenticator", "YunLemon:TRAVIS_CACHE_1", "title", "body")

    }

    @Test
    void testGetDefaultBranchName() {
        GithubUtil.getDefaultBranchName("YunLemon/datasafe")
        GithubUtil.getDefaultBranchName("iamazy/elasticsearch-sql")
    }

    @Test
    void testCrawlMVNRepository() {
        String url = "https://repo1.maven.org/maven2/io/floodplain/streams-api/maven-metadata.xml"
        GithubUtil.crawlMVNRepository(url, "zc.xml")

        url = "https://repo1.maven.org/maven2/org/eclipse/sw360/src-licenses/maven-metadata.xml"
        GithubUtil.crawlMVNRepository(url, "zc1.xml")
    }
}
