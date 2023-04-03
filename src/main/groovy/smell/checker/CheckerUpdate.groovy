package smell.checker

import groovy.io.FileType
import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import smell.StateFlag
import smell.checker.gradle.BuildGradleChecker
import smell.checker.gradle.GradleChecker
import smell.checker.maven.MavenChecker
import smell.checker.maven.POMChecker
import smell.checker.maven.TestChecker
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

import static util.GradleUtil.*
import static util.MavenUtil.*
import static util.TravisUtil.*

class CheckerUpdate {

    static void updateGradleSmell() throws Exception{
        List<GradleStrategy>  gradleStrategies = [GradleStrategy.GRADLE_PARALLEL_TEST,
                                                  GradleStrategy.GRADLE_FORK_TEST,
                                                  GradleStrategy.GRADLE_REPORT_GENERATION,
                                                  GradleStrategy.GRADLE_COMPILER_DAEMON,
                                                  GradleStrategy.GRADLE_INCREMENTAL_COMPILATION]
        for(GradleStrategy gradleStrategy : gradleStrategies){
            BuildGradleChecker.check(gradleStrategy)
        }
    }

    static void updateMavenSmell() throws Exception{
//        List<MavenStrategy> mavenStrategies =[MavenStrategy.MAVEN_PARALLEL_TEST,
//                                              MavenStrategy.MAVEN_FORK_TEST,
//                                              MavenStrategy.MAVEN_REPORT_GENERATION,
//                                              MavenStrategy.MAVEN_COMPILER_DAEMON,]

        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()

            List<Repository> repositories = new ArrayList<>()
            for (Repository repository: MysqlUtil.getRepositories(session)){
                if(repository.buildTool==1){
                    repositories << repository
                }
            }

            def errorRepo = []
            def strategy = MavenStrategy.MAVEN_FORK_TEST
            for (Repository repository : repositories) {
                String repoPath = Paths.get(Util.codeDirectoryPath, repository.repoName.replace('/','@')).normalize().toString()
                println("正在处理 ${repository.repoName}")
                try {
                    def flag = MavenChecker.check(repository,repoPath,strategy)
                    if(strategy == MavenStrategy.MAVEN_PARALLEL_TEST){
                        repository.setParallelTest(flag.getValue())
                    }
                    else if(strategy == MavenStrategy.MAVEN_FORK_TEST){
                        repository.setMavenForkTest(flag.getValue())
                    }
                    else if(strategy == MavenStrategy.MAVEN_REPORT_GENERATION){
                        repository.setMavenReportGeneration(flag.getValue())
                    }
                    else if(strategy == MavenStrategy.MAVEN_COMPILER_DAEMON){
                        repository.setMavenCompilerDaemon(flag.getValue())
                    }
                    session.update(repository)
                } catch (Exception e) {
                    errorRepo << repository.repoName
                    e.printStackTrace()
                }
                println("\n")
            }
            tx.commit()
            session.close()
            println("error size:" + errorRepo.size())
            errorRepo.each {println(it)}
        }
    }

    static void getTravisCache(){
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                String repoPath = Paths.get(Util.codeDirectoryPath, repository.getRepoName().replace("/", "@")).toString()
                String ymlFilePath = Paths.get(repoPath, ".travis.yml")
                File ymlFile = new File(ymlFilePath)
                if (!ymlFile.exists()) {
                    throw new Exception("不存在文件${ymlFilePath}}")
                }
                println(repository.getRepoName()+"的Cache情况：")
                TravisChecker.cacheCheck(ymlFilePath)
                println("")
            }
            tx.commit()
        }
    }

    static void getMavenParallelTest(){
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.getBuildTool() == 1){
                    try{
                        POMChecker checker = new TestChecker(repository)
                        Map<String, String> testConfigurations = checker.rootPom.getTestConfigurations()
                        String parallel = testConfigurations.get("parallel")
                        if (parallel == null){
                            continue
                        }
                        String useUnlimitedThreads = testConfigurations.get("useUnlimitedThreads")
                        String perCoreThreadCount = testConfigurations.get("perCoreThreadCount")
                        String threadCount = testConfigurations.get("threadCount")
                        String threadCountClasses = testConfigurations.get("threadCountClasses")
                        String threadCountMethods = testConfigurations.get("threadCountMethods")
                        String threadCountSuites = testConfigurations.get("threadCountSuites")
                        println(repository.getRepoName())
                        println("<parallel> :" + parallel)
                        println("<useUnlimitedThreads> :" + useUnlimitedThreads)
                        println("<perCoreThreadCount> :" + perCoreThreadCount)
                        println("<threadCount> :" + threadCount)
                        println("<threadCountClasses> :" + threadCountClasses)
                        println("<threadCountMethods> :" + threadCountMethods)
                        println("<threadCountSuites> :" + threadCountSuites)

                        if (useUnlimitedThreads == "false" || useUnlimitedThreads == null){
                            if (perCoreThreadCount=="false"){
                                println("情况1")
                            }
                            if ( threadCount == "0"){
                                println("情况2")
                            }
                            if ( threadCount == "1"){
                                println("情况3")
                            }
                        }
                        println("\n\n")

                    } catch(Exception e) {
                        println("error repository_id: " + repository.getId())
                        e.printStackTrace()
                    }
                }

            }
            tx.commit()
        }
    }

    static void getGradleForkTest(){
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for(Repository repository: repositories)
            {
                if(repository.buildTool==2){
                    println(repository.getRepoName())
                    println("gradleReportGeneration字段：" + repository.gradleReportGeneration)
                    BuildGradleChecker.check(repository,GradleStrategy.GRADLE_REPORT_GENERATION)
                }
            }
            tx.commit()
        }
    }

    static void updateGradleOptions(List<String> repos){
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            for (String repoName : repos) {
                println("处理${repoName}")
                def javaFiles = JVMLanguagesFileNumChecker.getFilesNum(repoName)
                println(javaFiles + "\n")
//                if(repository.parallelExecution==null){
//                    flag = GradleOptionChecker.gradleChecker(repoPath,repoName,GradleStrategy.PARALLEL_BUILDS)
//                    repository.setParallelExecution(flag.getValue())
////                    println("PARALLEL_BUILDS:${flag.toString()}")
//                }
//                if (repository.fileSystemWatch ==null) {
//                    flag = GradleOptionChecker.gradleChecker(repoPath,repoName,GradleStrategy.FILE_SYSTEM_WATCHING)
//                    repository.setFileSystemWatch(flag.getValue())
////                    println("FILE_SYSTEM_WATCHING:${flag.toString()}")
//                }
//                if (repository.configureOnDemand ==null) {
//                    flag = GradleOptionChecker.gradleChecker(repoPath,repoName,GradleStrategy.CONFIGURATION_ON_DEMAND)
//                    repository.setConfigureOnDemand(flag.getValue())
////                    println("CONFIGURATION_ON_DEMAND:${flag.toString()}")
//                }
//                if (repository.gradleCache ==null) {
//                    flag = GradleOptionChecker.gradleChecker(repoPath,repoName,GradleStrategy.CACHING)
//                    repository.setGradleCache(flag.getValue())
////                    println("CACHING:${flag.toString()}")
//                }
//                if (repository.gradleDaemon ==null) {
//                    flag = GradleOptionChecker.gradleChecker(repoPath,repoName,GradleStrategy.GRADLE_DAEMON)
//                    repository.setGradleDaemon(flag.getValue())
////                    println("GRADLE_DAEMON:${flag.toString()}")
//                }
            }
            tx.commit()
        }
    }

    static void updateContainTest(){
        Session session = SessionUtil.getSession()
        Transaction tx = session.beginTransaction()

        List<Repository> repositories = new ArrayList<>()
        for (Repository repository: MysqlUtil.getRepositories(session)){
            if(repository.buildTool==2 && repository.containTest == null){
                repositories << repository
            }
        }
        println("size:" + repositories.size())
        int updateCount = 0
        for(Repository repository: repositories)
        {
            String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString()
            def findTest = findTestFile(repoPath)
            if(findTest){
                updateCount++
                repository.setContainTest(1)
                session.update(repository)
            }else{
                println(repository.repoName +  " : 没有检测到测试文件")
            }
        }
        println("更新的项目数量：" + updateCount)
        tx.commit()
        session.close()
    }

    static boolean findTestFile(String repoPath){
        File repo = new File(repoPath)
        def list = []
        repo.eachFileRecurse(FileType.FILES){
            if(it.name.endsWith("Test.java")){
                list << it
            }
        }
        if(list.size()>0){
            return true
        }
        return false
    }



    static List<GradleStrategy> gradleSmellsChecker(Repository repository){
        if(repository.buildTool!=2){
            return null
        }
        def smells = []
        // properties
        if(repository.parallelExecution==0 || (repository.parallelExecution==2 && repository.multiModule)){
            smells << GradleStrategy.PARALLEL_BUILDS
        }

        if(repository.fileSystemWatch==0 || (repository.fileSystemWatch==2 && GradleChecker.compareVersion(repository.version, "7.0") < 0)){
            smells << GradleStrategy.FILE_SYSTEM_WATCHING
        }

        if(repository.configureOnDemand==0 || (repository.configureOnDemand==2 && repository.multiModule)){
            smells << GradleStrategy.CONFIGURATION_ON_DEMAND
        }

        if(repository.gradleDaemon==0 || (repository.gradleDaemon==2 && GradleChecker.compareVersion(repository.version, "3.0") < 0)){
            smells << GradleStrategy.GRADLE_DAEMON
        }

        if(repository.gradleCache==0 || repository.gradleCache==2){
            smells << GradleStrategy.CACHING
        }

        // Build
        if(repository.gradleCompilerDaemon==0 || (repository.gradleCompilerDaemon==2 && repository.javaFilesNum>=1000)){
            smells << GradleStrategy.GRADLE_COMPILER_DAEMON
        }

        if(repository.gradleIncrementalCompilation==0 || (repository.gradleIncrementalCompilation==2 && GradleChecker.compareVersion(repository.version, "4.10") < 0)){
            smells << GradleStrategy.GRADLE_INCREMENTAL_COMPILATION
        }

        if(repository.parallelTest==0 || (repository.parallelTest==2 && repository.containTest) ){
            smells << GradleStrategy.GRADLE_PARALLEL_TEST
        }

        if(repository.gradleReportGeneration==0 || (repository.gradleReportGeneration==2 && repository.containTest) ){
            smells << GradleStrategy.GRADLE_REPORT_GENERATION
        }

        if(repository.gradleForkTest==0 || (repository.gradleForkTest==2 && repository.containTest) ){
            smells << GradleStrategy.GRADLE_FORK_TEST
        }
        return smells
    }

    static List<MavenStrategy> mavenSmellsChecker(Repository repository){
        if(repository.buildTool!=1){
            return null
        }
        def smells = []
        if(repository.parallelExecution==0 || (repository.parallelExecution==2 && repository.multiModule)){
            smells << MavenStrategy.MAVEN_PARALLEL_EXECUTION
        }

        if(repository.mavenCompilerDaemon==0 || (repository.mavenCompilerDaemon==2 && repository.javaFilesNum>=1000)){
            smells << MavenStrategy.MAVEN_COMPILER_DAEMON
        }

        if(repository.parallelTest==0 || (repository.parallelTest==2 && repository.containTest) ){
            smells << MavenStrategy.MAVEN_PARALLEL_TEST
        }

        if(repository.mavenReportGeneration==0 || (repository.mavenReportGeneration==2 && repository.containTest) ){
            smells << MavenStrategy.MAVEN_REPORT_GENERATION
        }

        if(repository.mavenForkTest==0 || (repository.mavenForkTest==2 && repository.containTest) ){
            smells << MavenStrategy.MAVEN_FORK_TEST
        }
        return smells
    }

    static List<TravisStrategy> travisSmellsChecker(Repository repository){
        def smells = []
        if(repository.travisCache==null ||!repository.travisCache){
            smells << TravisStrategy.TRAVIS_CACHE
        }

        if(repository.travisFastFinish==null||!repository.travisFastFinish){
            if(repository.travisAllowFailures){
                smells << TravisStrategy.TRAVIS_FAST_FINISH
            }
        }

        if(repository.travisRetry){
            smells << TravisStrategy.TRAVIS_RETRY
        }

        if(repository.travisGitDepth=='false'){
            smells << TravisStrategy.TRAVIS_SHALLOW_CLONE
        }
        if(repository.travisGitDepth!=null && repository.travisGitDepth.isInteger() && repository.travisGitDepth.toInteger()>50){
            smells << TravisStrategy.TRAVIS_SHALLOW_CLONE
        }

        if(repository.travisWait!=null){
            smells << TravisStrategy.TRAVIS_WAIT
        }
        return smells
    }

    static void run(){
        Map<Repository,List<Integer>> ReposWithSmells = new HashMap<>()
        int gradleRepoSize = 0
        int mavenRepoSize = 0
        for(Repository repository:MysqlUtil.getRepositories()){
            if(repository.buildTool==2){
                gradleRepoSize++
                def gradleSmells = gradleSmellsChecker(repository)
                def travisSmells = travisSmellsChecker(repository)
                def smellsNum = [0,gradleSmells.size(),travisSmells.size()]
                ReposWithSmells.put(repository,smellsNum)
            }else if(repository.buildTool==1){
                mavenRepoSize++
                def mavenSmells = mavenSmellsChecker(repository)
                def travisSmells = travisSmellsChecker(repository)
                def smellsNum = [mavenSmells.size(),0,travisSmells.size()]
                ReposWithSmells.put(repository,smellsNum)
            }
        }
        println("gradle repos size:" + gradleRepoSize)
        println("maven repos size:" + mavenRepoSize)

        println(ReposWithSmells.size())
//        println("travis repos size:" + gradleRepoSize+mavenRepoSize)

        // 统计 [] --->[无smell, 一个smell, 两个及以上smell, 最多包含多少个smell]
        def statistic = [
                ("TRAVIS"):new int[4],
                ("MAVEN"):new int[4],
                ("GRADLE"):new int[4],
                ("ALL"):new int[4]
        ]

        Closure countSmellsNum ={int[] statisticNum, Integer smellsNum->
            if(smellsNum==0){
                statisticNum[0]++
            }else if(smellsNum==1){
                statisticNum[1]++
            }else{
                statisticNum[2]++
            }
            if(smellsNum>statisticNum[3]){
                statisticNum[3]=smellsNum
            }
            return statisticNum
        }

        ReposWithSmells.each {repository,repoSmellsNum->
            if(repository.buildTool==2){
                //gradle项目
                def gradleSmellsNum = repoSmellsNum[1]
                def gradleStatisticNum = countSmellsNum.call(statistic.get("GRADLE"),gradleSmellsNum)
                statistic.put("GRADLE",gradleStatisticNum)
            }else{
                //maven项目
                def mavenSmellsNum = repoSmellsNum[0]
                def mavenStatisticNum = countSmellsNum.call(statistic.get("MAVEN"),mavenSmellsNum)
                statistic.put("MAVEN",mavenStatisticNum)
            }
            def travisSmellsNum = repoSmellsNum[2]
            def travisStatisticNum = countSmellsNum.call(statistic.get("TRAVIS"),travisSmellsNum)
            statistic.put("TRAVIS",travisStatisticNum)

            def allSmellsNum = repoSmellsNum[0]+repoSmellsNum[1]+repoSmellsNum[2]
            def allStatisticNum = countSmellsNum.call(statistic.get("ALL"),allSmellsNum)
            statistic.put("ALL",allStatisticNum)
        }

        statistic.each {
            println(it.key.toString())
            println("无smell: " + it.value[0])
            println("一个smell: " + it.value[1])
            println("两个及以上smell: " + it.value[2])
            println("项目含有最多的smell: " + it.value[3])
        }
    }


    static void main(String[] args) {

    }
}
