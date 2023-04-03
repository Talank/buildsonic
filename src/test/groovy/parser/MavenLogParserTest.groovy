package parser
import org.junit.jupiter.api.Test
import smell.fixer.Maven.POMFixer

import java.nio.file.Paths

import static org.junit.Assert.*
class MavenLogParserTest {
    @Test
    void testParse() {
        String path = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "88250@solo.txt").normalize().toString();
        def (buildSuccess, containUnusedDependency, logDependenciesMap) = MavenLogParser.parse(path)
        assertTrue(buildSuccess)
        assertTrue(containUnusedDependency)
        assertEquals(1, logDependenciesMap.size())

        path = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "eBay@jsonex.txt").normalize().toString();
        (buildSuccess, containUnusedDependency, logDependenciesMap) = MavenLogParser.parse(path)
        assertTrue(buildSuccess)
        assertTrue(containUnusedDependency)
        assertEquals(7, logDependenciesMap.size())
        logDependenciesMap.each {k, v ->
            println(k)
            v.each {println(it)}
        }

        path = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "eldur@jwbf.txt").normalize().toString();
        (buildSuccess, containUnusedDependency, logDependenciesMap) = MavenLogParser.parse(path)
        assertTrue(buildSuccess)
        assertTrue(containUnusedDependency)
        assertEquals(1, logDependenciesMap.size())
        logDependenciesMap.each {k, v ->
            println(k)
            v.each {println(it)}
        }
    }

    @Test
    void testParse1() throws Exception {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "apache@commons-io.xml").normalize().toString();
        POMFixer xmlParser = new POMFixer(path);
        System.out.println(xmlParser.getSurefirePlugin());
    }
}
