package smell.fixer.Maven

import model.Repository
import smell.StateFlag
import smell.checker.maven.CompilationChecker
import smell.checker.maven.TestChecker
import util.MavenUtil

import java.nio.file.Paths

class CompilationFixer {
    static boolean compilerDaemon(String rootXmlPath) {
        POMFixer fixer = new POMFixer(rootXmlPath)
        //没有配置compiler plugin的话，暂时不处理了
        if (fixer.getCompilerPlugin() == null)
            return false
        Map<String, String> map = new LinkedHashMap<>();
        map.put("fork", "true")
        fixer.editCompilerNode(map)
        fixer.printToFile(rootXmlPath)
        return true
    }

    static boolean incrementalCompilation(String rootXmlPath) {
        POMFixer fixer = new POMFixer(rootXmlPath)
        //没有配置compiler plugin的话，暂时不处理了
        if (fixer.getCompilerPlugin() == null)
            return false
        //fixer.updateCompilerNode("useIncrementalCompilation", "true")
        Map<String, String> map = new LinkedHashMap<>()
        map.put("useIncrementalCompilation", "true")
        fixer.editCompilerNode(map)
        fixer.printToFile(rootXmlPath)
        return true
    }



    static boolean fixer(String repoPath, MavenUtil.MavenStrategy strategy) {
        String rootXmlPath = Paths.get(repoPath, "pom.xml").toString()
        if(strategy == MavenUtil.MavenStrategy.MAVEN_COMPILER_DAEMON) {
            return compilerDaemon(rootXmlPath)
        } else if(strategy == MavenUtil.MavenStrategy.MAVEN_INCREMENTAL_COMPILATION) {
            return incrementalCompilation( rootXmlPath)
        }
        return false
    }

    static boolean fixer(Repository originRepository, String forkRepoName, String reposDir, MavenUtil.MavenStrategy strategy) {
        String repoPath = Paths.get(reposDir, forkRepoName.replace('/', '@'))
        return fixer(repoPath, strategy)
    }

    static void main(String[] args) {
////        String rootXmlPath1 = Paths.get(Util.forkDirectoryPath,'xiayingfeng@cat','pom.xml')
//        String rootXmlPath2 = Paths.get(System.getProperty("user.dir"), 'resources','pom.xml')
//        boolean ans = compilerDaemon(rootXmlPath2)
////        boolean ans = compilerDaemon(rootXmlPath2)

    }
}
