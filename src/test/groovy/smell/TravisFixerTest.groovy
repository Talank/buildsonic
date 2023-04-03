package smell

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import smell.checker.TravisChecker

import java.nio.file.Paths

import static org.junit.Assert.*

class TravisFixerTest {
    @Test
    void testModifyGitDepth1() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "cyclestreets@android.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "out1.yml").normalize().toString();
        String content = TravisFixer.modifyGitDepth(ymlFilePath)
        def map = new Yaml().load(content)
        assertEquals(3, map.get("git").get("depth"))
        new File(outFilePath).write(content)
    }

    @Test
    void testModifyGitDepth2() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "m2e-code-quality@m2e-code-quality.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "out2.yml").normalize().toString();
        String content = TravisFixer.modifyGitDepth(ymlFilePath)
        def map = new Yaml().load(content)
        assertEquals(3, map.get("git").get("depth"))
        new File(outFilePath).write(content)
    }

    @Test
    void testModifyGitDepth3() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "test3.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "out3.yml").normalize().toString();
        String content = TravisFixer.modifyGitDepth(ymlFilePath)
        def map = new Yaml().load(content)
        assertEquals(3, map.get("git").get("depth"))
        new File(outFilePath).write(content)
    }

    @Test
    void testModifyGitDepth4() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "test4.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "shallowClone", "out4.yml").normalize().toString();
        String content = TravisFixer.modifyGitDepth(ymlFilePath)
        def map = new Yaml().load(content)
        assertEquals(3, map.get("git").get("depth"))
        new File(outFilePath).write(content)
    }

    @Test
    void testTravisRetry1() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisRetry", "gephi@gephi.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisRetry", "out1.yml").normalize().toString();
        String content = TravisFixer.removeTravisRetry(ymlFilePath)
        new File(outFilePath).write(content)
        List<String> lines = content.split('\n')
        lines.each {
            if (it.trim().startsWith('#') == false)
                assertFalse(it.contains('travis_retry'))
        }
    }
    @Test
    void testTravisWait1() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisWait", "apache@maven-surefire.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisWait", "out1.yml").normalize().toString();
        String content = TravisFixer.removeTravisWait(ymlFilePath)
        new File(outFilePath).write(content)
//        List<String> lines = content.split('\n')
//        lines.each {
//            if (it.trim().startsWith('#') == false)
//                assertFalse(it.contains('travis_retry'))
//        }
    }

    @Test
    void testTravisWait2() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisWait", "KengoTODA@what-is-maven.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "travisWait", "out2.yml").normalize().toString();
        String content = TravisFixer.removeTravisWait(ymlFilePath)
        new File(outFilePath).write(content)
        List<String> lines = content.split('\n')
        lines.each {
            if (it.trim().startsWith('#') == false)
                assertFalse(it.contains('travis_retry'))
        }
    }

    @Test
    void testfastFinish1() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "bndtools@bndtools.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "out1.yml").normalize().toString();
        String content = TravisFixer.addFastFinish(ymlFilePath)
        new File(outFilePath).write(content)
        def  (allow_failures, fast_finish) = TravisChecker.fastFinishCheck(outFilePath)
        assertTrue(allow_failures)
        assertTrue(fast_finish)
    }

    @Test
    void testfastFinish2() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "eclipse-ee4j@eclipselink.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "out2.yml").normalize().toString();
        String content = TravisFixer.addFastFinish(ymlFilePath)
        new File(outFilePath).write(content)
        def  (allow_failures, fast_finish) = TravisChecker.fastFinishCheck(outFilePath)
        assertTrue(allow_failures)
        assertTrue(fast_finish)
    }

    @Test
    void testfastFinish3() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "intendia-oss@rxjava-gwt.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "out3.yml").normalize().toString();
        String content = TravisFixer.addFastFinish(ymlFilePath)
        new File(outFilePath).write(content)
        def  (allow_failures, fast_finish) = TravisChecker.fastFinishCheck(outFilePath)
        assertTrue(allow_failures)
        assertTrue(fast_finish)
    }

    @Test
    void testfastFinish4() {
        String ymlFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "apache@commons-csv.yml").normalize().toString()
        String outFilePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "travis", "fastFinish", "out4.yml").normalize().toString();
        String content = TravisFixer.addFastFinish(ymlFilePath)
        new File(outFilePath).write(content)
        def  (allow_failures, fast_finish) = TravisChecker.fastFinishCheck(outFilePath)
        assertTrue(allow_failures)
        assertTrue(fast_finish)
    }
}
