package smell.checker.maven

import maven.POM
import maven.POMTree
import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

class MultiModuleChecker {

    static boolean isMultiModule(String repoPath) {
        POMTree pomTree = new POMTree(repoPath);
        POM pom = null
        try {
            pom = pomTree.createPomTree()
        } catch(Exception exception) {
            exception.printStackTrace()
        }
        if (pom == null)
            return false
        if (pom.getPackaging().equals('pom') && pom.getAggregatorPoms().size() > 1) {
            return true
        } else {
            return false
        }
    }

    static boolean isMultiModule(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
       return isMultiModule(repoPath)
    }

    static void isMultiModule() {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool != 1)
                    continue
                println("处理${repository.getRepoName()}")
                boolean flag = isMultiModule(repository)
                repository.setMultiModule(flag)
            }
            tx.commit()
        }

    }
}
