package smell.fixer

import org.junit.jupiter.api.Test
import smell.fixer.Maven.MavenProperty
import smell.fixer.Maven.POMFixer

import java.nio.file.Paths

import static org.junit.jupiter.api.Assertions.*;

class POMFixerTest {
    @Test
    void testGetLatestReleaseVersion() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "antlr4-runtime-metadata.xml").normalize().toString()
        POMFixer fixer = new POMFixer(filePath)
        assertEquals("4.9.3", fixer.getLatestReleaseVersion())
        assertFalse(fixer.containsProperties())

        filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "find-sec-bugs@find-sec-bugs.xml").normalize().toString()
        fixer = new POMFixer(filePath)
        assertTrue(fixer.containsProperties())
        List<MavenProperty> properties = fixer.getProperties()
        assertEquals(2, properties.size())
        assertEquals("UTF-8", properties[0].getContent())
        assertEquals("UTF-8", properties[1].getContent())
    }

    @Test
    void testModifyProperties() {
        String inPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "in2.xml").normalize().toString()
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "out2.xml").normalize().toString()
        POMFixer fixer = new POMFixer(inPath)
        assertFalse(fixer.containsProperties())
        //fixer.insertProperties()
        fixer.insertProperty("disableXmlReport", "true")
        fixer.printToFile(outPath)
        fixer = new POMFixer(outPath)
        assertTrue(fixer.containsProperties())
    }

    @Test
    void testModifyProperties1() {
        String inPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "in3.xml").normalize().toString()
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "out3.xml").normalize().toString()
        POMFixer fixer = new POMFixer(inPath)
        assertTrue(fixer.containsProperties())
        fixer.insertProperty("disableXmlReport", "true")
        fixer.printToFile(outPath)
        fixer = new POMFixer(outPath)
        assertEquals(3, fixer.getProperties().size())
    }

    @Test
    void testUnusedDependencies() {
        String inPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "in4.xml").normalize().toString()
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "out4.xml").normalize().toString()
        POMFixer fixer = new POMFixer(inPath)
        assertEquals(13, fixer.getDependencies().size())
        assertEquals(2, fixer.getProperties().size())
        fixer.insertProperty("disableXmlReport", "true")
        fixer.removeDependency("com.github.spotbugs", "spotbugs")
        fixer.removeDependency("com.h3xstream.findsecbugs", "findsecbugs-samples-kotlin")
        Map<String, String> map = new LinkedHashMap<>();
        map.put("parallel", "classes")
        map.put("useUnlimitedThreads", "true")
        fixer.editSurefireNode(map)
        fixer.editCompilerNode(map)
        fixer.updateCompilerNode("useIncrementalCompilation", "true")
        fixer.printToFile(outPath)
        fixer = new POMFixer(outPath)
        assertEquals(3, fixer.getProperties().size())
        assertEquals(11, fixer.getDependencies().size())
    }
}
