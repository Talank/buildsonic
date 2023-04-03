package parser

import model.Repository
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.idea.IdeaProject
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

class Tool {
    static void dep(String repoPath) {
        File file = new File(repoPath)
        println(file.getAbsolutePath())
        ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(file)
                .connect();

        try {
            //connection.newBuild().setStandardOutput(System.out).forTasks("clean", "dependencies").run();
            IdeaProject project = connection.getModel(IdeaProject.class);
            project.getModules().size()

        } catch(Exception e) {
            e.printStackTrace()
        } finally {
            connection.close();
        }
    }

    static void main(String[] args) {
        List<Repository> repositories = MysqlUtil.getRepositories();
        for (Repository repository : repositories) {
            String repoName = repository.repoName
            String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
            File file = new File(Paths.get(repoPath, "build.gradle").toString())
            if (file.exists()) {
                dep(repoPath)
            }
        }

    }
}
