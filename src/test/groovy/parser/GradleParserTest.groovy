package parser

import org.junit.jupiter.api.Test
import static org.junit.Assert.*
import java.nio.file.Paths

class GradleParserTest {
    @Test
    void testGetGradleFileContents() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "9.gradle").normalize().toString();
        GradleParser parser = null
        try {
            parser = new GradleParser(filePath)
        } catch(Exception e) {
            e.printStackTrace()
        }
        if (parser != null) {
            println(parser.getAllDependencies())
        }
    }

    @Test
    void testConnectbotConnectbotApp() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "connectbot@connectbot@app.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = null
        parser = new GradleParser(filePath)
        List<GradleDependency> dependencies = parser.getAllDependencies()
        assertEquals(28, dependencies.size())
    }

    @Test
    void testFreenetPlugin() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "freenet@plugin-WebOfTrust.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = null
        parser = new GradleParser(filePath)
        List<GradleDependency> dependencies = parser.getAllDependencies()
        println(dependencies)
        assertEquals(0, dependencies.size())
    }

    @Test
    void testApereoCas() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "apereo@cas.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = null
        parser = new GradleParser(filePath)
        List<GradleDependency> dependencies = parser.getAllDependencies()
        println(dependencies)
        assertEquals(0, dependencies.size())
    }

    @Test
    void testInclude1() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "AbhinayMe@currency-edittext@settings.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = new GradleParser(filePath)
        GradleVisitor visitor = parser.getVisitor()
        assertEquals(2, visitor.getModuleNames().size())
    }

    @Test
    void testInclude2() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "ballerina-platform@ballerina-lang@settings.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = new GradleParser(filePath)
        GradleVisitor visitor = parser.getVisitor()
        print(visitor.getModuleNames())
        assertEquals(107, visitor.getModuleNames().size())
    }

    @Test
    void testInclude3() {
        String filePath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "gradleFiles", "CarGuo@GSYVideoPlayer@settings.gradle").normalize().toString();
        println(filePath)
        GradleParser parser = new GradleParser(filePath)
        GradleVisitor visitor = parser.getVisitor()
        print(visitor.getModuleNames())
        assertEquals(12, visitor.getModuleNames().size())
    }
}
