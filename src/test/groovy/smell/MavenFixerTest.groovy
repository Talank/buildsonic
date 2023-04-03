package smell

import model.Repository
import org.junit.jupiter.api.Test
import smell.fixer.Maven.MavenDependency
import smell.fixer.Maven.MavenPlugin
import parser.OptionParser
import smell.fixer.Maven.POMFixer
import smell.fixer.Maven.DependencyFixer
import smell.fixer.Maven.ParallelBuildFixer
import util.MysqlUtil
import util.Util
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Paths

class MavenFixerTest {
    @Test
    void testParallelExecutionFixerFile1() {
        String originRepoName = "phax/as2-lib"
        String repoPath = Paths.get(Util.codeDirectoryPath, originRepoName.replace("/", "@")).toString()
        OptionParser optionParser = new OptionParser(repoPath, originRepoName)
        List<String> filePaths = ParallelBuildFixer.getParallelExecutionEditedPaths(optionParser)
        print(filePaths)
        assertEquals(1, filePaths.size())
    }

    @Test
    void testParallelExecutionFixerFile2() {
        String originRepoName = "antlr/antlr4"
        String repoPath = Paths.get(Util.codeDirectoryPath, originRepoName.replace("/", "@")).toString()
        OptionParser optionParser = new OptionParser(repoPath, originRepoName)
        List<String> filePaths = ParallelBuildFixer.getParallelExecutionEditedPaths(optionParser)
        print(filePaths)
        assertEquals(24, filePaths.size())
    }

    @Test
    void testDynamicVersionFixer1() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        for (Repository repository : repositories) {
            if (repository.buildTool == 1 && repository.dynamicVersion) {
                String repoPath = Paths.get(Util.codeDirectoryPath, repository.repoName.replace('/', '@'))
                try{
                    DependencyFixer.dynamicVersionFixer(repoPath)
                } catch(Exception e) {
                    //e.printStackTrace()
                }
            }
        }
    }

    @Test
    void testDynamicVersionFixer2() {
        String repoName = "xuminwlt/j360-boot-app-all"
        String forkName = "ChenZhangg/j360-boot-app-all"
        String repoPath = Paths.get(Util.forkDirectoryPath, forkName.replace("/", "@")).toString()
        DependencyFixer.dynamicVersionFixer(repoPath)
    }

    @Test
    void testUnusedDependeniesFixer() {
        String originRepoName = "phax/as2-lib"
        String forkRepoName = "ChenZhangg/as2-lib"
        String repoPath = Paths.get(Util.forkDirectoryPath, forkRepoName.replace("/", "@")).toString()
        boolean flag = DependencyFixer.unusedDependeniesFixer(repoPath, originRepoName)
        println(flag)

        POMFixer xmlParser = new POMFixer(Paths.get(repoPath, "as2-demo-webapp", "pom.xml").toString())

    }

    @Test
    void testUnusedDependeniesFixer1() {
        String originRepoName = "keeps/roda"
        String forkRepoName = "ChenZhangg/roda"
        String repoPath = Paths.get(Util.forkDirectoryPath, forkRepoName.replace("/", "@")).toString()
        boolean flag = DependencyFixer.unusedDependeniesFixer(repoPath, originRepoName)
        println(flag)
    }

    @Test
    void testUnusedDependeniesFixer2() {
        String originRepoName = "medcl/elasticsearch-analysis-pinyin"
        String repoPath = Paths.get(Util.forkDirectoryPath, originRepoName.replace("/", "@")).toString()
        boolean flag = DependencyFixer.unusedDependeniesFixer(repoPath, originRepoName)
        println(flag)
    }

    @Test
    void testParallelTestFixer() {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "mpatric@mp3agic.xml").normalize().toString();
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "out.xml").normalize().toString();
        POMFixer xmlParser = new POMFixer(path);
        List<MavenDependency> dependencies = xmlParser.getDependencies()
        assertEquals(2, dependencies.size())
        println(dependencies)
        List<MavenPlugin> plugins = xmlParser.getPlugins()
        plugins.each {
            println(it)
        }
        assertEquals(7, plugins.size())
        MavenPlugin mavenPlugin = xmlParser.getSurefirePlugin()
        assertEquals(0, mavenPlugin.getConfigurationMap().size())
        xmlParser.removeDependency('nl.jqno.equalsverifier', 'equalsverifier')
        xmlParser.editSurefireNode()
        xmlParser.printToFile(outPath)
    }
    @Test
    void testParallelTestFixer1() {
        String path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "find-sec-bugs@find-sec-bugs.xml").normalize().toString();
        String outPath = Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "maven", "out1.xml").normalize().toString();
        POMFixer xmlParser = new POMFixer(path);
        List<MavenDependency> dependencies = xmlParser.getDependencies()
        assertEquals(13, dependencies.size())
        List<MavenPlugin> plugins = xmlParser.getPlugins()
        plugins.each {
            println(it)
        }
        assertEquals(7, plugins.size())
        MavenPlugin mavenPlugin = xmlParser.getSurefirePlugin()
        assertNull(mavenPlugin.getConfigurationMap().get("parallel"))
        xmlParser.removeDependency('org.slf4j', 'slf4j-api')
        xmlParser.editSurefireNode()
        xmlParser.printToFile(outPath)
    }
    @Test
    void testParallelTestFixer2() {
        String path = Paths.get(System.getProperty("user.dir"), "resources", "dynamicVersion", "cn.vertxup@vertx-co.xml").normalize().toString();
        POMFixer xmlParser = new POMFixer(path);
        assertEquals('0.6.2', xmlParser.getLatestReleaseVersion())
    }
}
