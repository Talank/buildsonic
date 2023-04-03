package smell.checker.maven

import model.Repository
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

class UnusedDependenciesChecker {
    static void unusedDependeniesChecker(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        String outFilePath = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "${repository.getRepoName().replace('/', '@')}.txt").normalize().toString();
//        File file = new File(outFilePath)
//        if (file.exists()) {
//            file.delete()
//        }
        PrintStream stream = new PrintStream(outFilePath)
        List<String> com = ["mvn", "dependency:analyze"]
        Process process = com.execute(null, new File(repoPath))
        process.waitForProcessOutput(stream, stream)
    }

    static void unusedDependeniesChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        List<String> sp = []
        for (Repository repository : repositories) {
            if (repository.buildTool == 1 && repository.id > 866838) {
                println(repository.id + " " + repository.repoName)
                unusedDependeniesChecker(repository)
            }
        }
    }
}
