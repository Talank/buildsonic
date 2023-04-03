package smell

import model.Repository
import org.junit.jupiter.api.Test
import parser.YmlParser
import smell.checker.TravisChecker
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

import static org.junit.Assert.*

class TravisCheckerTest {
    @Test
    void testShallowCloneCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        for (Repository repository : repositories) {
            //println(repository.repoName)
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            List<String> lines = new File(ymlFilePath).readLines()
            lines.each {
                if (it.trim().startsWith("depth:"))
                    println(it)
            }
            def shallowCloneValue = TravisChecker.shallowCloneCheck(repository)
            String depth = repository.getTravisGitDepth()
            if (shallowCloneValue == null) {
                assertNull(depth)
                map["null"] = map.getOrDefault("null", 0) + 1
            } else if (shallowCloneValue == false) {
                assertEquals("false", depth)
                map[depth] = map.getOrDefault(depth, 0) + 1
            } else {
                assertEquals(shallowCloneValue, depth.toInteger())
                if (shallowCloneValue == 50) {
                    map["=50"] = map.getOrDefault("=50", 0) + 1
                } else if (shallowCloneValue > 50) {
                    map[">50"] = map.getOrDefault(">50", 0) + 1
                } else {
                    map["<50"] = map.getOrDefault("<50", 0) + 1
                }
            }
        }
        println(map)
    }

    @Test
    void testRetryCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        List<String> spRepos = ["OleksandrKucherenko/meter", "jamorham/xDrip-plus", "NightscoutFoundation/xDrip"]
        for (Repository repository : repositories) {
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            String content = new File(ymlFilePath).text

            if (spRepos.contains(repository.repoName)) {
                map["false"] = map.getOrDefault("false", 0) + 1
                continue
            }

            if (content.contains("travis_retry")) {
                assertTrue("${ymlFilePath}", repository.getTravisRetry())
                map["true"] = map.getOrDefault("true", 0) + 1
            } else {
                assertFalse("${ymlFilePath}", repository.getTravisRetry())
                map["false"] = map.getOrDefault("false", 0) + 1
            }
        }
        println(map)
    }

    @Test
    void testYaml() {
        //这个文件解析有问题
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "bigbluebutton@bigbluebutton.yml").normalize().toString();
        def map = YmlParser.parse(ymlFilePath)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "getsentry@sentry-java.yml").normalize().toString();
        def result = TravisChecker.cacheCheck(ymlFilePath)
        assertNotNull(result)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "irmen@Pyrolite.yml").normalize().toString();
        result = TravisChecker.cacheCheck(ymlFilePath)
        assertNotNull(result)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fossasia@phimpme-android.yml").normalize().toString();
        result = TravisChecker.cacheCheck(ymlFilePath)
        assertNotNull(result)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "apache@kylin.yml").normalize().toString();
        map = YmlParser.parse(ymlFilePath)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "maxamel@GDH.yml").normalize().toString();
        map = YmlParser.parse(ymlFilePath)
        //println(map)

        ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "sorcix@sIRC.yml").normalize().toString();
        map = YmlParser.parse(ymlFilePath)
        println(map)
        TravisChecker.fastFinishCheck(ymlFilePath)
    }

    @Test
    void testWaitCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        List<String> spRepos = ["bigbluebutton/bigbluebutton", "unfoldingWord-dev/ts-android", "typetools/checker-framework", "jamorham/xDrip-plus", "NightscoutFoundation/xDrip", "AndProx/AndProx", "icyphy/ptII"]
        for (Repository repository : repositories) {
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            String content = new File(ymlFilePath).text
            List<String> lines = new File(ymlFilePath).readLines()
            lines.removeIf {
                it.contains("travis_wait") == false || it.trim().startsWith("#")
            }

            if (spRepos.contains(repository.repoName)) {
                map["null"] = map.getOrDefault("null", 0) + 1
                continue
            }

            if (lines.size() > 0) {
                assertNotNull("${ymlFilePath}", repository.getTravisWait())
                map["notNull"] = map.getOrDefault("notNull", 0) + 1
                List<Integer> integerList = repository.getTravisWait()
                for (Integer i : integerList) {
                    if (i != 20) {
                        assertTrue(content.contains("travis_wait ${i}"))
                    }
                }
            } else {
                assertNull("${ymlFilePath}", repository.getTravisWait())
                map["null"] = map.getOrDefault("null", 0) + 1
            }


        }
        println(map)
    }

    @Test
    void testCacheCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        List<String> spRepos = ["HubSpot/slack-client", "ribot/easy-adapter", "optimizely/android-sdk", "VictorAlbertos/ReactiveCache"]
        for (Repository repository : repositories) {
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            List<String> lines = new File(ymlFilePath).readLines()
            lines.removeIf {
                it.trim().startsWith("cache:") == false || it.trim().startsWith("#")
            }
            def m = YmlParser.parse(ymlFilePath)
            if (spRepos.contains(repository.repoName) || m == null) {
                map["null"] = map.getOrDefault("null", 0) + 1
                continue
            }
            if (lines.size() == 0) {
                assertNull("${ymlFilePath}", repository.getTravisCache())
                map["null"] = map.getOrDefault("null", 0) + 1
            } else if (m.get("cache") == false){
                assertFalse("${ymlFilePath}", repository.getTravisCache())
                map["false"] = map.getOrDefault("false", 0) + 1
            } else {
                assertTrue("${ymlFilePath}", repository.getTravisCache())
                map["true"] = map.getOrDefault("true", 0) + 1
            }


        }
        println(map)
    }

    @Test
    void testAllowFailureCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        List<String> spRepos = ["eclipse-ee4j/krazo"]
        for (Repository repository : repositories) {
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            List<String> lines = new File(ymlFilePath).readLines()
            lines.removeIf {
                it.contains("allow_failures:") == false || it.trim().startsWith("#")
            }

            def m = YmlParser.parse(ymlFilePath)
            if (m == null || spRepos.contains(repository.repoName)) {
                continue
            }

            if (lines.size() > 0) {
                assertTrue("${ymlFilePath}", repository.getTravisAllowFailures())
            } else {
                assertFalse("${ymlFilePath}", repository.getTravisAllowFailures())
            }

        }
        println(map)
    }

    @Test
    void testFastFinishCheck() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<String, Integer> map = new HashMap()
        List<String> spRepos = []
        for (Repository repository : repositories) {
            String ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize().toString()
            List<String> lines = new File(ymlFilePath).readLines()
            lines.removeIf {
                it.contains("fast_finish:") == false || it.trim().startsWith("#")
            }

            def m = YmlParser.parse(ymlFilePath)
            if (m == null || spRepos.contains(repository.repoName)) {
                continue
            }

            if (lines.size() > 0) {
                String s = lines.join()
                if (s.contains("true"))
                    assertTrue("${ymlFilePath}", repository.getTravisFastFinish())
                else
                    assertFalse("${ymlFilePath}", repository.getTravisFastFinish())
            } else {
                assertNull("${ymlFilePath}", repository.getTravisFastFinish())
            }

        }
        println(map)
    }
}
