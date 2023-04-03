package smell.checker.maven

import maven.POM
import maven.POMParser
import maven.POMTree
import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import smell.StateFlag
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

import util.MavenUtil.MavenStrategy

class TestChecker extends POMChecker {

    TestChecker(String repoPath) {
        super(repoPath)
    }

    TestChecker(Repository repository) {
        super(repository)
    }

    @Override
    StateFlag check(MavenStrategy strategy) {
        //这里只检测的根pom，后面根据实际情况看是否要检测其它pom文件
//        if (this.rootPom==null){
//            return null
//        }
//        Map<String, String> testConfigurations = this.rootPom.getTestConfigurations();
//        Closure predicate = POMChecker.predicatesMap.get(strategy)
//        return predicate.call(testConfigurations)
        Closure predicate = POMChecker.predicatesMap.get(strategy)
        def open = false

        for(String pomPath : this.pomPaths){
            try{
                POM pom = new POMParser().parse(pomPath)
                Map<String, String> testConfigurations = pom.getTestConfigurations()
                if (testConfigurations.keySet().size()==0){
                    continue
                }
                def flag = predicate.call(testConfigurations)
                if (flag == StateFlag.CLOSE){
                    this.smellPomPaths << pomPath
                }else if (flag == StateFlag.OPEN){
                    open = true
                    break
                }
            }catch (Exception e){
                println(pomPath+" error")
            }
        }
        if (open){
            return StateFlag.OPEN
        }
        if (this.smellPomPaths.size()!=0){
            this.smellPomPaths.each {println(it)}
            return StateFlag.CLOSE
        }
        return StateFlag.DEFAULT
    }

    static void check() {
        try (Session session = SessionUtil.getSession()) {
            //Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool != 1)
                    continue
                println("处理${repository.getRepoName()}")
                try {
                    POMChecker checker = new TestChecker(repository)
                    checker.check(MavenStrategy.MAVEN_PARALLEL_TEST)
                } catch (Exception e) {
                    e.printStackTrace()
                }
                //boolean flag = check(repository)
                //repository.setParallelTest(flag)
            }
            //tx.commit()
        }
    }
}
