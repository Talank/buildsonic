package smell.fixer.Maven

import model.Repository
import smell.StateFlag
import smell.checker.maven.TestChecker
import util.MavenUtil

import java.nio.file.Paths
import static util.MavenUtil.*

class TestFixer {
    static boolean parallelTest(String rootXmlPath) {
        POMFixer fixer = new POMFixer(rootXmlPath)
        //没有配置surefire plugin的话，暂时不处理了
        if (fixer.getSurefirePlugin() == null)
            return false
        Map<String, String> map = new LinkedHashMap<>();
        map.put("parallel", "classes")
        map.put("useUnlimitedThreads", "true")
        fixer.editSurefireNode(map)
        fixer.printToFile(rootXmlPath)
        return true
    }

    static boolean forkTest(String rootXmlPath) {
        POMFixer fixer = new POMFixer(rootXmlPath)
        //没有配置surefire plugin的话，暂时不处理了
        if (fixer.getSurefirePlugin() == null)
            return false
        Map<String, String> map = new LinkedHashMap<>();
        map.put("forkCount", "1.5C")
        fixer.editSurefireNode(map)
        fixer.printToFile(rootXmlPath)
        return true
    }

    static boolean report(String rootXmlPath) {
        POMFixer fixer = new POMFixer(rootXmlPath)
        //没有配置surefire plugin的话，暂时不处理了
        if (fixer.getSurefirePlugin() == null)
            return false
        Map<String, String> map = new LinkedHashMap<>();
        map.put("disableXmlReport", '${closeTestReports}')
        fixer.editSurefireNode(map)
        fixer.insertProperty("closeTestReports", "true")
        fixer.printToFile(rootXmlPath)
        return true
    }


    static boolean fixer(String repoPath, MavenStrategy strategy) {
        String rootXmlPath = Paths.get(repoPath, "pom.xml").toString()
        if(strategy == MavenStrategy.MAVEN_PARALLEL_TEST) {
            return parallelTest(rootXmlPath)
        } else if(strategy == MavenStrategy.MAVEN_FORK_TEST) {
            return forkTest(rootXmlPath)
        } else if (strategy == MavenStrategy.MAVEN_REPORT_GENERATION) {
            return report(rootXmlPath)
        }
        return false
    }

    static boolean fixer(Repository originRepository, String forkRepoName, String reposDir, MavenStrategy strategy) {
        String repoPath = Paths.get(reposDir, forkRepoName.replace('/', '@'))
        return fixer(repoPath, strategy)
    }
}
