package maven;

import org.junit.jupiter.api.Test;
import util.Util;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class POMTreeTest {

    @Test
    void testNormal() {
        String path = Paths.get(Util.codeDirectoryPath, "miltonio@milton2").normalize().toString();
        assertEquals("/Users/zhangchen/projects/sequence/repository/miltonio@milton2", path);
        path = Paths.get(Util.codeDirectoryPath, "..", "miltonio@milton2").normalize().toString();
        assertEquals("/Users/zhangchen/projects/sequence/miltonio@milton2", path);
        path = Paths.get(Util.codeDirectoryPath, "../miltonio@milton2").normalize().toString();
        assertEquals("/Users/zhangchen/projects/sequence/miltonio@milton2", path);
    }
    @Test
    void createPomTree() throws Exception {
        String repoPath = Paths.get(Util.codeDirectoryPath, "miltonio@milton2").normalize().toString();
        POMTree pomTree = new POMTree(repoPath);
        POM rootPom = pomTree.createPomTree();
        assertEquals("io.milton", rootPom.getGroupId());
        assertEquals("milton", rootPom.getArtifactId());
        assertEquals("3.0.0.194", rootPom.getVersion());
        assertEquals("milton", rootPom.getName());
        assertEquals("pom", rootPom.getPackaging());
        assertNull(rootPom.getParent());
        HashMap<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("servlet.version", "2.4");
        expectedProperties.put("mime-util.version", "2.1.3");
        expectedProperties.put("project.build.sourceEncoding", "UTF-8");
        expectedProperties.put("easy-mock.version", "3.1");
        expectedProperties.put("netbeans.hint.license", "apache20");
        assertEquals(5, rootPom.getRawProperties().size());
        assertEquals(5, rootPom.getProperties().size());
        assertEquals(expectedProperties, rootPom.getRawProperties());
        assertEquals(expectedProperties, rootPom.getProperties());

        rootPom.getTestConfigurations().forEach((key, value) -> System.out.println(key + ":" + value));
        assertEquals(0, rootPom.getTestConfigurations().size());
        List<String> expectedGroupIds = new ArrayList<>();
        expectedGroupIds.add("javax.servlet");
        expectedGroupIds.add("org.slf4j");
        expectedGroupIds.add("junit");
        expectedGroupIds.add("org.slf4j");
        List<String> depGroupIds = new ArrayList<>();
        rootPom.getDependencies().forEach(dependency -> depGroupIds.add(dependency.getGroupID()));
        assertEquals(4, rootPom.getDependencies().size());
        assertEquals(expectedGroupIds, depGroupIds);
        assertEquals(7, rootPom.getModules().size());
        assertEquals(7, rootPom.getAggregatorPoms().size());

        assertEquals(2, rootPom.getCompilationConfigurations().size());
        assertEquals("1.8", rootPom.getCompilationConfigurations().get("source"));
        assertEquals("1.8", rootPom.getCompilationConfigurations().get("target"));

        assertEquals(8, pomTree.getPomList().size());

        POM childPom = rootPom.getAggregatorPoms().get(0);
        assertEquals(rootPom, childPom.getParent());
        assertEquals("jar", childPom.getPackaging());
        assertEquals("io.milton", childPom.getParentGroupId());
        assertEquals("milton", childPom.getParentArtifactId());
        //System.out.println(childPom);
    }
    @Test
    void createPomTree1() throws Exception {
        String repoPath = Paths.get(Util.codeDirectoryPath, "oshi@oshi").normalize().toString();
        POMTree pomTree = new POMTree(repoPath);
        POM rootPom = pomTree.createPomTree();
        assertEquals("com.github.oshi", rootPom.getGroupId());
        assertEquals("oshi-parent", rootPom.getArtifactId());
        assertEquals("5.8.1-SNAPSHOT", rootPom.getVersion());
        assertEquals(1, rootPom.getCompilationConfigurations().size());
        assertEquals("false", rootPom.getCompilationConfigurations().get("useIncrementalCompilation"));
    }
}