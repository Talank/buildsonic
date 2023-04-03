package smell.checker.maven

import maven.Dependency
import maven.POM
import maven.POMTree
import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import util.GithubUtil
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

class DynamicVersionChecker {
    static boolean containDynamicVersion(String repoPath) {
        POMTree pomTree = new POMTree(repoPath);
        POM pom = null
        try {
            pom = pomTree.createPomTree()
        } catch(Exception exception) {
            exception.printStackTrace()
        }
        if (pom == null)
            return false

        List<Dependency> dependencies = pomTree.getDependencies();
//        for (Map.Entry<String, List<Dependency>> i : pomTree.getDependenciesMap().entrySet()) {
//            System.out.println(i.getKey());
//            i.getValue().forEach(System.out::println);
//        }

        boolean flag = false
        flag = dependencies.any {
            String version = it.getVersion()
            version.equals("RELEASE") ||version.equals("LATEST") || version.endsWith("SNAPSHOT")
        }
        return flag
    }

    static boolean containDynamicVersion(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        return containDynamicVersion(repoPath)
    }

    static void containDynamicVersion() {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool != 1)
                    continue
                println("处理${repository.getRepoName()}")
                boolean flag = containDynamicVersion(repository)
                repository.setDynamicVersion(flag)
            }
            tx.commit()
        }

    }

    static void crawlDynamicVersion(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        POMTree pomTree = new POMTree(repoPath);
        POM pom = null
        try {
            pom = pomTree.createPomTree()
        } catch(Exception exception) {
            exception.printStackTrace()
        }
        if (pom == null)
            return

        List<Dependency> dependencies = pomTree.getDependencies();

        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion()
            if (version.equals("RELEASE") ||version.equals("LATEST") || version.endsWith("SNAPSHOT")) {
                for (String baseUrl : pomTree.getRepositoryUrls()) {
                    if (baseUrl.contains("wiquery.googlecode.com") || baseUrl.contains("104.236.246.108"))
                        continue
                    String url = "${baseUrl}${dependency.groupID.replace('.', '/')}/${dependency.artifactID.replace('.', '/')}/maven-metadata.xml"
                    println(url)
                    String outXmlPath = Paths.get(System.getProperty("user.dir"), "resources", "dynamicVersion", "${dependency.groupID}@${dependency.artifactID}.xml").normalize().toString();
                    if (new File(outXmlPath).exists())
                        break
                    GithubUtil.crawlMVNRepository(url, outXmlPath)
                }
            }
        }
    }

    static void crawlDynamicVersion() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        for (Repository repository : repositories) {
            if (repository.id > 1142521 && repository.buildTool == 1 && repository.dynamicVersion) {
                println("处理${repository.getRepoName()}")
                crawlDynamicVersion(repository)
            }
        }


    }
}
