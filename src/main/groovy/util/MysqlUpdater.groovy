package util

import model.Repository
import org.hibernate.CacheMode
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.Transaction
import org.hibernate.query.Query

import java.nio.file.Paths

class MysqlUpdater {
    static void run() {
        List<String> removeRepoNames = ["apereo/cas", "jOOQ/jOOR", "apache/ode"/*archived*/, "speedment/speedment", "Ifsttar/NoiseCapture", "Netflix/iceberg", "Netflix/metacat"]
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                String fullRepoName = repository.getRepoName()
                def (userName, repoName) = Util.splitFullRepoName(fullRepoName)
                if (userName == 'freenet') {
                    println(fullRepoName)
                    repository.setIgnoreRepo(true)
                }

                if (fullRepoName == 'apache/commons-collections' || fullRepoName == 'eclipse-ee4j/jersey' || fullRepoName == 'pravega/pravega' || fullRepoName == 'tinkerpop/gremlin' || fullRepoName == 'marytts/marytts' || fullRepoName == 'bioinformatics-ua/dicoogle' || fullRepoName == 'Blazebit/blaze-persistence' || fullRepoName == 'orientechnologies/orientdb-gremlin' || fullRepoName == 'Syncleus/aparapi' || fullRepoName == 'jpos/jPOS' || fullRepoName == 'jpos/jPOS-EE' || fullRepoName == 'SeleniumHQ/selenium' || fullRepoName == "seata/seata" || removeRepoNames.contains(fullRepoName)) {
                    println(fullRepoName)
                    repository.setIgnoreRepo(true)
                }
            }
            tx.commit()
        }
    }



    static void main(String[] args) {
        /*
        String repoName = "stephenh/joist"
        int javaFilesNum = getFilesNum(repoName)
        println(javaFilesNum) //782
         */
    }
}
