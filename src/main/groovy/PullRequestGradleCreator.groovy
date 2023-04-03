import smell.StateFlag
import smell.fixer.Gradle.GradleExplicitSmellFixer
import smell.fixer.Gradle.GradlePropertiesFixer
import smell.fixer.Gradle.BuildGradleFixer
import model.Repository
import smell.checker.gradle.GradleChecker
import util.*
import static util.GradleUtil.*

import java.nio.file.Files
import java.nio.file.Paths
/**
 * static void createGradlePullRequest() 从数据库中选择项目进行修复，选择的参数怎么定
 * static Boolean useGradleGradleStrategy() 确认需要执行的修复策略，调用GradleFileChecker的static STATE_FLAG gradleChecker(String repoPath, GradleGradleStrategy strategy)
 * static List<String> getDescription() 描述：需要根据选择的策略来决定，具体内容目前为空
 */

class PullRequestGradleCreator {

    static Boolean run(Repository repository, GradleCategory category, boolean isExplicit,GradleStrategy strategy) {
        String originRepoName = repository.getRepoName()
        // fork原项目
        GithubUtil.forkRepo(originRepoName)

        //得到默认的分支名
        String defaultBranchName = GithubUtil.getDefaultBranchName(originRepoName)

        //fork项目名
        def (userName, repoName) = Util.splitFullRepoName(originRepoName)
        String forkRepoName = "${GithubUtil.USER_NAME}/${repoName}"

        // 存储项目代码的目录，如果不存在就创建
        Util.createDir(Util.forkDirectoryPath.toString())
        //clone项目代码到指定文件夹
        String repoPath = Paths.get(Util.forkDirectoryPath, forkRepoName.replace("/", "@")).toString()
        if (!Files.exists(Paths.get(repoPath))) {
            HubUtil.cloneRepo(repoPath, forkRepoName)
        }
        //更新fork仓库代码
        //git remote add upstream
        GitUtil.setUpstream(repoPath, originRepoName)
        HubUtil.fetchAndMergeUpStream(repoPath, defaultBranchName)

        // 已存在分支的话，会切换到相应分支
        String branchName = GitUtil.createAndCheckoutBranch(repoPath, "Modify_GRADLE", defaultBranchName)
        def flag = useGradleStrategy(repository, repoPath, originRepoName, strategy)
        if (flag!=StateFlag.CLOSE){
            println("检测出不为显式引入：" + flag.toString())
            return false
        }
        //获取要执行的修复策略
//        List<GradleStrategy> strategies = getGradleStrategies(repository, repoPath, originRepoName, category, isExplicit)
//        println("修复策略数量： "+ strategies.size())
//        println("修复种类：" + category.toString())
//        if (strategies.size() == 0) {
//            return
//        }
        List<GradleStrategy> strategies = [strategy]
        //进行修复操作 写入到文件中
        applyFix(repoPath,originRepoName, strategies, category,isExplicit)

        //提交commit
        String commitMessage = 'Improve GRADLE build Performance'
        GitUtil.addAndCommit(repoPath, commitMessage)

        //push
        HubUtil.push(repoPath, branchName)

        //发起pull request
        String head = "${GithubUtil.USER_NAME}:${branchName}"
        def (title, description, outFilePaths) = getDescription(strategies)
        return GithubUtil.pullRequest(originRepoName, head, defaultBranchName, title, description, outFilePaths)
    }

    static String getPRTitle(GradleStrategy strategy){
        def parallelTest = [
                'Parallel test execution maxParallelForks',
                'Enable parallel test feature',
                'Take full advantage of multi-core cpus while execute test',
        ]
        def parallelBuild = [
                'Parallel builds can improve the build speed',
                'Reduce total build time for a multi-project build',
                'Provide faster feedback for execution by Parallel Build',
        ]
        def gradleDaemon = [
                'Executes the builds much more quickly by Gradle Daemon',
                'Gradle Daemon is important for performance',
                'Improve build speed by reusing computations from previous builds'
        ]
        def configurationOnDemand = [
                'Configure only projects that are relevant for requested tasks',
                'How about enable Gradle Configuration on Demand feature',
                'Improve GRADLE build Performance'
        ]
        int randomNum = new Random().nextInt(3)
        if(strategy== GradleStrategy.GRADLE_PARALLEL_TEST){
            return parallelTest[randomNum]
        }
        if(strategy== GradleStrategy.PARALLEL_BUILDS){
            return parallelBuild[randomNum]
        }
        if(strategy== GradleStrategy.GRADLE_DAEMON){
            return gradleDaemon[randomNum]
        }
        if(strategy== GradleStrategy.CONFIGURATION_ON_DEMAND){
            return configurationOnDemand[randomNum]
        }

        return ""
    }

    static List<String> getDescription(List<GradleStrategy> strategies) {
        String title = getPRTitle(strategies[0])
        title = title==""?'Improve GRADLE build Performance':title
        String description = ""
        List<String> outFilePaths = []

        for (GradleStrategy strategy : strategies) {
            outFilePaths << Util.getPullRequestListFilePath(strategy)

            if (strategy == GradleStrategy.GRADLE_PARALLEL_TEST) {
                description += "\n[Parallel test execution maxParallelForks](https://docs.gradle.org/current/userguide/performance.html#parallel_test_execution). Gradle can run multiple test cases in parallel by setting `maxParallelForks`.\n"
            } else if (strategy == GradleStrategy.GRADLE_FORK_TEST) {
                description += "\n[Process forking options](https://docs.gradle.org/current/userguide/performance.html#forking_options). Gradle will run all tests in a single forked VM by default. This can be problematic if there are a lot of tests or some very memory-hungry ones. We can fork a new test VM after a certain number of tests have run by setting `forkEvery`.\n"
            } else if (strategy == GradleStrategy.GRADLE_REPORT_GENERATION) {
                description += "\n[Disable report generation](https://docs.gradle.org/current/userguide/performance.html#report_generation). We can conditionally disable it by setting `reports.html.required = false; reports.junitXml.required = false`. If you need to generate reports, add `-PcreateReports` to the end of Gradle's build command line.\n"
            } else if (strategy == GradleStrategy.GRADLE_COMPILER_DAEMON) {
                description += "\n[Compiler daemon](https://docs.gradle.org/current/userguide/performance.html#compiler_daemon). The Gradle Java plugin allows you to run the compiler as a separate process by setting `options.fork = true`. This feature can lead to much less garbage collection and make Gradle’s infrastructure faster. This project has more than 1000 source files. We can consider enabling this feature.\n"
            } else if(strategy == GradleStrategy.GRADLE_INCREMENTAL_COMPILATION){
                description += "\n[Incremental compilation](https://docs.gradle.org/current/userguide/performance.html#incremental_compilation). Gradle recompile only the classes that were affected by a change. This feature is the default since Gradle 4.10. For an older versions, we can activate it by setting `options.incremental = true`.\n"
            } else if (strategy == GradleStrategy.PARALLEL_BUILDS){
                description += "\n[Parallel builds](https://docs.gradle.org/current/userguide/multi_project_configuration_and_execution.html#sec:parallel_execution). This project contains multiple modules. Parallel builds can improve the build speed by executing tasks in parallel. We can enable this feature by setting `org.gradle.parallel=true`.\n"
            } else if (strategy == GradleStrategy.FILE_SYSTEM_WATCHING){
                description += "\n[File system watching](https://blog.gradle.org/introducing-file-system-watching). Since Gradle 6.5, File system watching was introduced which can help to avoid unnecessary I/O. This feature is the default since 7.0. For an older version, we can enable this feature by setting `org.gradle.vfs.watch=true`.\n"
            } else if (strategy == GradleStrategy.CONFIGURATION_ON_DEMAND){
                description += "\n[Configuration on demand](https://docs.gradle.org/current/userguide/multi_project_configuration_and_execution.html#sec:configuration_on_demand). Configuration on demand tells Gradle to configure modules that only are relevant to the requested tasks instead of configuring all of them. We can enable this feature by setting `org.gradle.configureondemand=true`.\n"
            } else if (strategy == GradleStrategy.CACHING){
                description += "\n[gradle caching](https://docs.gradle.org/current/userguide/build_cache.html). Shared caches can reduce the number of tasks you need to execute by reusing outputs already generated elsewhere. This can significantly decrease build times. We can enable this feature by setting `org.gradle.caching=true`.\n"
            } else if (strategy == GradleStrategy.GRADLE_DAEMON){
                description += "\n[Gradle daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html#header). The Daemon is a long-lived process that help to avoid the cost of JVM startup for every build. Since Gradle 3.0, Gradle daemon is enabled by default. For an older version, you should enable it by setting `org.gradle.daemon=true`.\n"
            }

        }

        description += '\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n'
        return [title, description, outFilePaths]
    }

    static StateFlag useGradleStrategy(Repository repository, String repoPath, String originRepoName, GradleStrategy strategy) {
        //抽取记录已发pr的文件，如果已经发给包含该strategy的pr，则返回false
        String contentFilePath = Util.getPullRequestListFilePath(strategy)
        File contentFile = new File(contentFilePath)
        String content = contentFile.exists() ? contentFile.text : ""
        if (content.contains(originRepoName)) {
            return null
        }
        //STATE_FLAG有三种状态： OPEN(1), DEFAULT(2), CLOSE(0)
        //当返回OPEN(1)时，说明原文件中已经有对应配置了
        //当返回CLOSE(0)时，说明原文件配置了第4或第5个策略并且设置为false（与修复策略的配置相反）
        //当返回DEFAULT(2)时，原文件没有相应的策略配置
        return GradleChecker.check(repository, repoPath, strategy)
    }

    static void applyFix(String repoPath,String originRepoName, List<GradleStrategy> strategies, GradleCategory category, boolean isExplicit){
        if(isExplicit){
            // 显式引入
            GradleExplicitSmellFixer fixer = new GradleExplicitSmellFixer(originRepoName,repoPath)
            fixer.fixer(strategies[0])
        }else{
            // 隐式引入
            if (category == GradleCategory.TEST || category == GradleCategory.COMPILATION || category == GradleCategory.FORK){
                println("开始修复build.gradle")
                BuildGradleFixer modifyGradle = new BuildGradleFixer(repoPath)
                modifyGradle.modifyGradle(category, strategies)
            } else if (category == GradleCategory.PROPERTIES){
                println("开始修复gradle.properties")
                GradlePropertiesFixer.modifyProperties(repoPath, strategies)
            }
        }
    }


    static List<GradleStrategy> getGradleStrategies(Repository repository, String repoPath, String originRepoName, GradleCategory category, boolean isExplicit) {
        List<GradleStrategy> strategies = []
        for (GradleStrategy strategy : strategiesOfCategory.get(category)) {
            def flag = useGradleStrategy(repository, repoPath, originRepoName, strategy)
            if (isExplicit && flag==StateFlag.CLOSE) {
                strategies << strategy
            }else if(!isExplicit && flag==StateFlag.DEFAULT){
                strategies << strategy
            }
        }
        return strategies
    }

    static Boolean createGradlePullRequest(GradleCategory category, Repository repository, boolean isExplicit, GradleStrategy strategy){
        String repoName = repository.repoName
        def PRCreated = false
        println("开始处理"+strategy.toString()+"策略的项目${repository.id}: ${repoName}")
        try {
            PRCreated = run(repository,category,isExplicit,strategy)
        } catch (Exception e) {
            e.printStackTrace()
        }
        println("处理完"+strategy.toString()+"策略的项目${repository.id}: ${repoName}")
        return PRCreated
    }

    static Map<GradleCategory, List<Repository>> getRepositoriesMap(){
        //从ID>42471且gradle_version不为null的gradle项目有1421个
        //分成三组,初始的id范围(42471，701383] 、(701383,1659915]、（1659915，6192497]  数量分别为500、500、421
        //第三组TEST(1659915，6192497] : (1659915，1739937]三个策略一起发，还剩427个项目，要将FORK单独提出来
        // (1739937，3153815] 300个项目   (3153815,6192497] 127个，这个区间用来发FORK
        Map<GradleCategory, List<Repository>> repositoriesMap = [(GradleCategory.PROPERTIES): [],
                                                                 (GradleCategory.COMPILATION): [],
                                                                 ]
//        for (Repository repository : MysqlUtil.getRepositories()) {
//            if(repository.buildTool == 2  && repository.version !=null){
//                if(repository.id > 506073 && repository.id <= 701383 ){
//                    if(repository.getJavaFilesNum()>=1000){
//                        repositoriesMap.get(GradleCategory.COMPILATION).add(repository)
//                    }else if(repository.getVersion().matches(~"^6\\.[5-9]+.*") && repositoriesMap.get(GradleCategory.PROPERTIES).size()<10 ){
//                        //Gradle verison范围在[6.5，7.0)有63个，对应策略：FILE_SYSTEM_WATCHING
//                        repositoriesMap.get(GradleCategory.PROPERTIES).add(repository)
//                    }
//                }else if(repository.id > 1639774 && repository.id <= 1659915){
//                    //java_file_num>=1000的在这个区间内有17个-->0个
//                    if(repository.getJavaFilesNum()>=1000){
//                        repositoriesMap.get(GradleCategory.COMPILATION).add(repository)
//                    }
//
//                }else if(repository.id > 1819945 && repository.id <= 3153815){
//                    if(repository.getJavaFilesNum()>=1000){
//                        //6个
//                        repositoriesMap.get(GradleCategory.COMPILATION).add(repository)
//                    }
////                    else {
////                        repositoriesMap.get(GradleCategory.TEST).add(repository)
////                    }
//                }else if(repository.id > 3580066 && repository.id <= 6192497 ){
//                    if(repository.getJavaFilesNum()>=1000){
//                        repositoriesMap.get(GradleCategory.COMPILATION).add(repository)
//                    }
////                    else{
////                        repositoriesMap.get(GradleCategory.FORK).add(repository)
////                    }
//                }
//            }
//        }
        File GradleCompileDaemon = new File(Util.getPullRequestListFilePath(GradleStrategy.GRADLE_COMPILER_DAEMON))
        def prExit = GradleCompileDaemon.readLines()
        for (Repository repository : MysqlUtil.getRepositories()) {
            if(repository.buildTool == 2) {
                if (repository.id > 313304 && repository.id <= 701383 && repository.version !=null) {
                    //Gradle verison范围在[1.0，3.0)有157个，对应策略：GRADLE_DAEMON
                    if (repository.getVersion().matches(~"^[1-2].*")) {
                        repositoriesMap.get(GradleCategory.PROPERTIES).add(repository)
                    }
                }
                if (repository.javaFilesNum>=1000){
                    //gradle && javaFilesNum>=1000的repo共有78个，还剩余36个
                    if(!prExit.any {it.contains(repository.repoName)}){
                        repositoriesMap.get(GradleCategory.COMPILATION).add(repository)
                    }
                }
            }
        }
        repositoriesMap.each {GradleCategory category, List<Repository> repositories ->
            println(category.toString() + "长度为：" + repositories.size())
        }
        return repositoriesMap
    }

    static Map<GradleStrategy, List<Repository>> getExplicitRepositoriesMap(){
        Map<GradleStrategy,List<Repository>> ExplicitRepoMap = [
                (GradleStrategy.PARALLEL_BUILDS):[],
                (GradleStrategy.GRADLE_PARALLEL_TEST):[],
                (GradleStrategy.GRADLE_DAEMON):[],
                (GradleStrategy.CONFIGURATION_ON_DEMAND):[]
        ]
        for (Repository repository : MysqlUtil.getRepositories()){
            if(repository.repoName=='spring-social/spring-social-google'){
                continue
            }
            if(repository.buildTool==2){
                if(repository.gradleDaemon==0){
                    // 74
                    ExplicitRepoMap.get(GradleStrategy.GRADLE_DAEMON).add(repository)
                }
                if(repository.parallelExecution==0){
                    // 11
                    ExplicitRepoMap.get(GradleStrategy.PARALLEL_BUILDS).add(repository)
                }
                if(repository.parallelTest==0){
                    // 15
                    ExplicitRepoMap.get(GradleStrategy.GRADLE_PARALLEL_TEST).add(repository)
                }
                if(repository.configureOnDemand==0){
                    // 18
                    ExplicitRepoMap.get(GradleStrategy.CONFIGURATION_ON_DEMAND).add(repository)
                }
//                if(repository.gradleReportGeneration==0){
//                    ExplicitRepoMap.get(GradleStrategy.GRADLE_REPORT_GENERATION).add(repository)
//                }
//                if(repository.gradleForkTest==0){
//                    ExplicitRepoMap.get(GradleStrategy.GRADLE_FORK_TEST).add(repository)
//                }
            }
        }
        return ExplicitRepoMap
    }

    static void createGradlePullRequest(boolean isExplicit) {
        if(isExplicit){
            println ("显式引入")
            def repositoriesMap = getExplicitRepositoriesMap()
            repositoriesMap.each { GradleStrategy strategy, List<Repository>repositories ->
                GradleCategory category = getGradleCategory(strategy)
                int count = 0
                for (int i = 0; i < repositories.size() && count<50; i++) {
                    String contentFilePath = Util.getPullRequestListFilePath(strategy)
                    File contentFile = new File(contentFilePath)
                    String content = contentFile.exists() ? contentFile.text : ""
                    if (content.contains(repositories[i].repoName)) {
                        continue
                    }
                    def PRCreated = createGradlePullRequest(category, repositories[i], isExplicit,strategy)
                    if(PRCreated){
                        count++
                        goSleep()
                    }
                    if (i == count - 1 || i == repositories.size()-1) {
                        println("处理" + category.toString() + "--${strategy.toString()}"+ "策略的最后一个项目的ID为：" + repositories[i].id +"\n\n")
                    }
                }
            }
        }else{
            def repositoriesMap = getRepositoriesMap()
            repositoriesMap.each { GradleCategory category, List<Repository>repositories ->
                for (int i = 0; i < count && i < repositories.size(); i++) {
                    createGradlePullRequest(category, repositories[i], isExplicit, null)
                    goSleep()
                    if (i == count - 1 || i == repositories.size()-1) {
                        println("处理" + category.toString() + "策略的最后一个项目的ID为：" + repositories[i].id +"\n\n")
                    }
                }
            }
        }
    }

    static void goSleep(){
        Random random = new Random()
        Long sleepTime = 30+ random.nextInt(15)
        println("休息"+sleepTime+"分钟")
        long start = System.currentTimeMillis()
        sleep(sleepTime*60000)     //1min=60000ms
        long end = System.currentTimeMillis()
        println("休息结束，实际休息时间："+(end-start)/60000 + "min")
    }

    static void main(String[] args) {
        createGradlePullRequest(true)
    }
}