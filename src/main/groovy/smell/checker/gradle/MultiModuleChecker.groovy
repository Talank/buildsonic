package smell.checker.gradle

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import parser.GradleParser
import parser.GradleVisitor
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

class MultiModuleChecker {
    static boolean isMultiModule(String repoPath) {
        String settingsGradlePath = Paths.get(repoPath, 'settings.gradle')
        File file = new File(settingsGradlePath)
        boolean flag = false
        if (file.exists()) {
            GradleParser parser = new GradleParser(settingsGradlePath)
            GradleVisitor visitor = parser.getVisitor()
            if (visitor.getModuleNames().size() > 1) {
                flag = true
            }
        }
        return flag
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
                if (repository.buildTool == 2) {
                    println("处理${repository.getRepoName()}")
                    boolean flag = isMultiModule(repository)
                    repository.setMultiModule(flag)
                }
            }
            tx.commit()
        }

    }
}
