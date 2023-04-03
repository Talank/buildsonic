package smell.checker


import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import smell.checker.gradle.GradleChecker
import util.MysqlUtil
import util.SessionUtil

import java.nio.file.Paths

import static util.MavenUtil.MavenStrategy
import static util.GradleUtil.GradleStrategy
import static util.TravisUtil.TravisStrategy

/**
 * 根据检测条件，分区项目是显式Explicit还是隐式Implicit
 */
class ExplicitAndImplicit {

    static void run(){
        for(GradleStrategy strategy: GradleStrategy.values()){
            getExplicitAndImplicitOfGradle(strategy)
        }
        for(MavenStrategy strategy: MavenStrategy.values()){
            getExplicitAndImplicitOfMaven(strategy)
        }
        for(TravisStrategy strategy: TravisStrategy.values()){
            getExplicitAndImplicitOfTravis(strategy)
        }
    }

    static void getExplicitAndImplicitOfGradle(GradleStrategy strategy){
        StringBuffer explicitInduce = new StringBuffer()  //显式引入smell
        StringBuffer implicitInduce = new StringBuffer()  //隐式引入smell
        StringBuffer explicitRemove = new StringBuffer()  //显式消除smell
        StringBuffer implicitRemove = new StringBuffer()  //隐式消除smell

        try (Session session = SessionUtil.getSession()) {
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if(repository.getBuildTool()==2){
                    if(strategy == GradleStrategy.CACHING){
                        Integer gradle_cache = repository.getGradleCache()
                        if( gradle_cache==null ||gradle_cache == 2) {
                            implicitInduce.append(repository.repoName).append("\n")
                        } else if( gradle_cache == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy ==GradleStrategy.CONFIGURATION_ON_DEMAND){
                        Integer configure_on_demand = repository.getConfigureOnDemand()
                        boolean multiModule = repository.getMultiModule()
                        if( configure_on_demand==null||configure_on_demand == 2) {
                            if(multiModule){
                                implicitInduce.append(repository.repoName).append("\n")
                            }else{
                                implicitRemove.append(repository.repoName).append("\n")
                            }
                        } else if( configure_on_demand == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy ==GradleStrategy.FILE_SYSTEM_WATCHING) {
                        Integer file_system_watch = repository.getFileSystemWatch()
                        if( file_system_watch==null || file_system_watch == 2) {
                            if (GradleChecker.compareVersion(repository.version, "7.0") >= 0 || GradleChecker.compareVersion(repository.version, "6.5") < 0) {
                                implicitRemove.append(repository.repoName).append("\n")
                            }else{
                                implicitInduce.append(repository.repoName).append("\n")
                            }
                        } else if( file_system_watch == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy ==GradleStrategy.PARALLEL_BUILDS) {
                        Integer parallel_execution = repository.getParallelExecution()
                        boolean multiModule = repository.getMultiModule()
                        if( parallel_execution == null || parallel_execution == 2) {
                            if(multiModule){
                                implicitInduce.append(repository.repoName).append("\n")
                            }else{
                                implicitRemove.append(repository.repoName).append("\n")
                            }
                        } else if( parallel_execution == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy==GradleStrategy.GRADLE_DAEMON){
                        Integer gradle_daemon = repository.getGradleDaemon()
                        if( gradle_daemon==null||gradle_daemon == 2) {
                            if (GradleChecker.compareVersion(repository.version, "3.0") >= 0) {
                                implicitRemove.append(repository.repoName).append("\n")
                            }else{
                                implicitInduce.append(repository.repoName).append("\n")
                            }
                        } else if( gradle_daemon == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy==GradleStrategy.GRADLE_COMPILER_DAEMON) {
                        Integer gradle_compiler_daemon = repository.getGradleCompilerDaemon()
                        if( gradle_compiler_daemon==null||gradle_compiler_daemon == 2) {
                            if(repository.javaFilesNum>=1000){
                                implicitInduce.append(repository.repoName).append("\n")
                            }else{
                                implicitRemove.append(repository.repoName).append("\n")
                            }
                        } else if( gradle_compiler_daemon == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy==GradleStrategy.GRADLE_INCREMENTAL_COMPILATION) {
                        Integer gradle_incremental_compilation = repository.getGradleIncrementalCompilation()
                        if( gradle_incremental_compilation==null||gradle_incremental_compilation == 2) {
                            if (GradleChecker.compareVersion(repository.version, "4.10") >= 0) {
                                implicitRemove.append(repository.repoName).append("\n")
                            }else{
                                implicitInduce.append(repository.repoName).append("\n")
                            }
                        } else if( gradle_incremental_compilation == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy==GradleStrategy.GRADLE_PARALLEL_TEST){
                        Integer parallel_test = repository.getParallelTest()
                        if( parallel_test==null||parallel_test == 2) {
                            implicitInduce.append(repository.repoName).append("\n")
                        } else if( parallel_test == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy==GradleStrategy.GRADLE_FORK_TEST){
                        Integer gradle_fork_test = repository.getGradleForkTest()
                        if( gradle_fork_test==null||gradle_fork_test == 2) {
                            implicitInduce.append(repository.repoName).append("\n")
                        } else if( gradle_fork_test == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                }
            }
        }
        String content = strategy.toString()+"\n\n显式CLOSE\n"+explicitInduce.toString()+"\n\n隐式CLOSE\n"+implicitInduce.toString()+
                "\n\n显式OPEN\n"+explicitRemove.toString()+"\n\n隐式OPEN\n"+implicitRemove.toString()
        saveFile(content,strategy.toString())
    }

    static void getExplicitAndImplicitOfMaven(MavenStrategy strategy){
        StringBuffer explicitInduce = new StringBuffer()  //显式引入smell
        StringBuffer implicitInduce = new StringBuffer()  //隐式引入smell
        StringBuffer explicitRemove = new StringBuffer()  //显式消除smell
        StringBuffer implicitRemove = new StringBuffer()  //隐式消除smell

        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if(repository.getBuildTool()==1){
                    if(strategy == MavenStrategy.MAVEN_PARALLEL_EXECUTION){
                        int parallel_execution = repository.getParallelExecution()
                        boolean multiModule = repository.getMultiModule()
                        if( parallel_execution == 2) {
                            if (multiModule){
                                implicitInduce.append(repository.repoName).append("\n")
                            }else{
                                implicitRemove.append(repository.repoName).append("\n")
                            }
                        } else if( parallel_execution == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                    if(strategy == MavenStrategy.MAVEN_PARALLEL_TEST){
                        int parallel_test = repository.getParallelTest()
                        if( parallel_test == 2) {
                            implicitInduce.append(repository.repoName).append("\n")
                        } else if( parallel_test == 0) {
                            explicitInduce.append(repository.repoName).append("\n")
                        } else {
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                }
            }
            tx.commit()
        }
        String content = strategy.toString()+"\n\n显式CLOSE\n"+explicitInduce.toString()+"\n\n隐式CLOSE\n"+implicitInduce.toString()+
                "\n\n显式OPEN\n"+explicitRemove.toString()+"\n\n隐式OPEN\n"+implicitRemove.toString()
        saveFile(content,strategy.toString())
    }

    static void getExplicitAndImplicitOfTravis(TravisStrategy strategy){
        StringBuffer explicitInduce = new StringBuffer()  //显式引入smell
        StringBuffer implicitInduce = new StringBuffer()  //隐式引入smell
        StringBuffer explicitRemove = new StringBuffer()  //显式消除smell
        StringBuffer implicitRemove = new StringBuffer()  //隐式消除smell

        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if(strategy == TravisStrategy.TRAVIS_CACHE){
                    Boolean travis_cache = repository.getTravisCache()
                    if( travis_cache == null) {
                        implicitInduce.append(repository.repoName).append("\n")
                    } else if( travis_cache) {
                        explicitRemove.append(repository.repoName).append("\n")
                    } else {
                        explicitInduce.append(repository.repoName).append("\n")
                    }
                }
                if(strategy == TravisStrategy.TRAVIS_FAST_FINISH){
                    Boolean travis_fast_finish = repository.getTravisFastFinish()
                    if( travis_fast_finish == null) {
                        if(repository.getTravisAllowFailures()) {
                            implicitInduce.append(repository.repoName).append("\n")
                        }else{
                            implicitRemove.append(repository.repoName).append("\n")
                        }
                    } else if( travis_fast_finish) {
                        explicitRemove.append(repository.repoName).append("\n")
                    } else {
                        explicitInduce.append(repository.repoName).append("\n")
                    }
                }
                if(strategy == TravisStrategy.TRAVIS_RETRY){
                    Boolean travis_retry = repository.getTravisRetry()
                    if( travis_retry == null) {
                        implicitRemove.append(repository.repoName).append("\n")
                    } else if( travis_retry) {
                        explicitInduce.append(repository.repoName).append("\n")
                    } else {
                        explicitRemove.append(repository.repoName).append("\n")
                    }
                }
                if(strategy == TravisStrategy.TRAVIS_SHALLOW_CLONE){
                    String travis_git_depth = repository.getTravisGitDepth()
                    if( travis_git_depth == "false") {
                        explicitInduce.append(repository.repoName).append("\n")
                    } else if( travis_git_depth == null) {
                        implicitInduce.append(repository.repoName).append("\n")
                    }
                    if(travis_git_depth.strip().isInteger()){
                        int depth = travis_git_depth.strip().toInteger()
                        if (depth >= 50){
                            explicitInduce.append(repository.repoName).append("\n")
                        }else{
                            explicitRemove.append(repository.repoName).append("\n")
                        }
                    }
                }
                if(strategy == TravisStrategy.TRAVIS_WAIT){
                    List<Integer> travis_wait = repository.getTravisWait()
                    if( travis_wait == null) {
                        implicitRemove.append(repository.repoName).append("\n")
                    } else{
                        explicitInduce.append(repository.repoName).append("\n")
                    }
                }
            }
            tx.commit()
        }
        String content = strategy.toString()+"\n\n显式CLOSE\n"+explicitInduce.toString()+"\n\n隐式CLOSE\n"+implicitInduce.toString()+
                "\n\n显式OPEN\n"+explicitRemove.toString()+"\n\n隐式OPEN\n"+implicitRemove.toString()
        saveFile(content,strategy.toString())
    }
    
    
    static void saveFile(String content, String fileName){
        String path = Paths.get(System.getProperty("user.dir"), "resources","EI", fileName+"@EI.txt").normalize().toString()
        def file = new File(path)
        file.withWriter('utf-8'){writer->
            writer.write content
        }
    }
}
