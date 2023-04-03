package smell.checker.maven

import model.Repository
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.hibernate.Session
import org.hibernate.Transaction
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class MavenVersionChecker {
    static String getMavenVersion(String filePath) {
        String version = null
        File file = new File(filePath)
        Pattern pattern = ~/maven-([\.\d]+)-(bin|all).zip/
        if (file.exists()) {
            FileBasedConfigurationBuilder<FileBasedConfiguration> builder = Util.getConfigurationBuilder(filePath)
            Configuration config = builder.getConfiguration();
            String distributionUrl = config.getString("distributionUrl")
            Matcher matcher = pattern.matcher(distributionUrl)
            if (matcher.find()) {
                version = matcher.group(1)
            }
        }
        return version
    }

    static String getMavenVersion(Repository repository) {
        String filePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".mvn", "wrapper", "maven-wrapper.properties").normalize().toString();
        return getMavenVersion(filePath)
    }

    static void getMavenVersion() {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.getBuildTool() == 1) {
                    String verison = getMavenVersion(repository)
                    println("处理${repository.getRepoName()}")
                    if(verison != null)
                        repository.setVersion(verison)
                }
            }
            tx.commit()
        }
    }
    static void main(String[] args) {
        getMavenVersion()
    }
}
