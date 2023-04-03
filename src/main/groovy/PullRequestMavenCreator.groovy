import groovy.json.JsonSlurper
import model.Repository
import smell.StateFlag
import smell.checker.maven.MavenChecker
import smell.checker.maven.POMChecker
import smell.checker.maven.TestChecker
import smell.fixer.Maven.CompilationFixer
import smell.fixer.Maven.DependencyFixer
import smell.fixer.Maven.MavenExplicitSmellFixer
import smell.fixer.Maven.ParallelBuildFixer
import smell.fixer.Maven.TestFixer
import util.*

import java.nio.file.Files
import java.nio.file.Paths

import static util.GradleUtil.getGradleCategory
import static util.MavenUtil.*

class PullRequestMavenCreator {

    static Boolean run(Repository repository, MavenCategory category, boolean isExplicit, MavenStrategy mavenStrategy) {
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
        println("repoPath:" + repoPath)
        if (!Files.exists(Paths.get(repoPath))) {
            HubUtil.cloneRepo(repoPath, forkRepoName)
        }
        //更新fork仓库代码
        //git remote add upstream
        GitUtil.setUpstream(repoPath, originRepoName)
        HubUtil.fetchAndMergeUpStream(repoPath, defaultBranchName)

        // 已存在分支的话，会切换到相应分支
        String branchName = GitUtil.createAndCheckoutBranch(repoPath, "Modify_MAVEN", defaultBranchName)

        if (isExplicit){
            def ok = applyFix(repoPath, originRepoName, mavenStrategy,isExplicit)
        }else{
            List<MavenStrategy> strategies = getMavenStrategies(repository, repoPath,originRepoName, category, isExplicit)
            if (strategies==null || strategies.size() == 0) {
                println("smell数量为0")
                return false
            }

            //进行修复操作 写入到文件中 并提交commit
            for (MavenStrategy strategy : strategies) {
                applyFix(repoPath, originRepoName, strategy,isExplicit)
            }

            if (strategies.size() == 0) {
                println("所有smell都修复失败")
                return
            }
        }

        String commitMessage = 'Improve MAVEN build Performance'
        GitUtil.addAndCommit(repoPath, commitMessage)

        //push
        HubUtil.push(repoPath, branchName)

        //发起pull request
        String head = "${GithubUtil.USER_NAME}:${branchName}"
        def (title, description, outFilePaths) = getDescription([mavenStrategy], originRepoName)
        return GithubUtil.pullRequest(originRepoName, head, defaultBranchName, title, description, outFilePaths)
    }

    static String getPRTitle(MavenStrategy strategy){
        def reportTitles = [
                'How about turning off the default build reports generation feature',
                            'Disable the default builds report generation function',
                            'Build reports are not always needed',
                            'Improve build performance by turning off report generation',
                            'Do not actively generate build reports when not needed',
        ]
        def forkTitles = ['Configure Maven Fork Test parameters',
                'Improve performance in the Test phase',
                'Run all the tests in multiple forked VM',
                'Dynamically adjust the number of Forked VM based on the number of CPU cores',
                'How about add more Forked VM when run all tests in build'
        ]
        def parallelTestTitles = [
                'Parallel test execution maxParallelForks',
                'Enable parallel test feature',
                'Take full advantage of multi-core cpus while execute test',
                'Improve Build Performance By Enable Parallel Test',
                'Running JUnit Tests in Parallel with Maven'
        ]
        int randomNum = new Random().nextInt(5)
        if(strategy==MavenStrategy.MAVEN_REPORT_GENERATION){
            return reportTitles[randomNum]
        }
        if(strategy==MavenStrategy.MAVEN_FORK_TEST){
            return forkTitles[randomNum]
        }
        if(strategy==MavenStrategy.MAVEN_PARALLEL_TEST){
            return parallelTestTitles[randomNum]
        }
        return null
    }

    static List<String> getDescription(List<MavenStrategy> strategies, String originRepoName) {
        String title = getPRTitle(strategies[0])
        title = title==null?'Improve MAVEN build Performance':title
        String description = ""
//        List<String> outFilePaths = []
        String outFilePaths = ""
        for (MavenStrategy strategy : strategies) {
            outFilePaths = Util.getPullRequestListFilePath(strategy)
            if (strategy == MavenStrategy.MAVEN_PARALLEL_EXECUTION) {
                description += "\n[Parallel builds in Maven 3](https://cwiki.apache.org/confluence/display/MAVEN/Parallel+builds+in+Maven+3) Maven 3.x has the capability to perform parallel builds.\n"
            } else if (strategy == MavenStrategy.MAVEN_FORK_TEST) {
                description += "\nMaven will run all tests in a single forked VM by default. This can be problematic if there are a lot of tests or some very memory-hungry ones. We can fork more test VM by setting `<fork>1.5C</fork>`.\n"
            } else if (strategy == MavenStrategy.MAVEN_REPORT_GENERATION) {
                description += "\nThat report generation takes time, slowing down the overall build. Reports are definitely useful, but do you need them every time you run the build. We can conditionally disable generating test reports by setting `<disableXmlReport>true<disableXmlReport>`. If you need to generate reports, just add `-DcloseTestReports=false` to the end of mvn build command.\n"
            } else if (strategy == MavenStrategy.MAVEN_COMPILER_DAEMON) {
                description += "\nMaven allows you to run the compiler as a separate process by setting `<fork>true</fork>`. This feature can lead to much less garbage collection and make Maven build faster. This project has more than 1000 source files. We can consider enabling this feature.\n"
            }
//            else if (strategy == MavenStrategy.MAVEN_INCREMENTAL_COMPILATION) {
//                description += "\nMaven can recompile only the classes that were affected by a change. This feature is the default. We can activate it by setting `<useIncrementalCompilation>true</useIncrementalCompilation>`.\n"
//            } else if (strategy == MavenStrategy.MAVEN_DYNAMIC_VERSION) {
//                description += '\nAccording to [Minimize dynamic and snapshot versions](https://docs.gradle.org/current/userguide/performance.html#minimize_dynamic_and_snapshot_versions), we should avoid using snapshot versions. And a build configuration should always specify exact versions of external libraries to make a build reproduceable. A lack of exact versions can cause problems when new versions of a dependency become available in the future that might introduce incompatible changes.\n'
//            } else if (strategy == MavenStrategy.MAVEN_UNUSED_DEPENDENCY) {
//                description += '\n[Apache Maven Dependency Plugin](https://maven.apache.org/plugins/maven-dependency-plugin/index.html) can be used to find unused dependencies. And I found following list. Maybe we can remove them.\n'
//                String path = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "${originRepoName.replace('/', '@')}.txt").normalize().toString();
//                def (buildSuccess, contain_unused_dependency, logDependencies) = parser.MavenLogParser.parse(path)
//                logDependencies.each {k, list ->
//                    description += k + "\n"
//                    list.each {description += it.toString() + "\n"}
//                }
//                description += "\n"
//            }
            else if (strategy == MavenStrategy.MAVEN_PARALLEL_TEST) {
                description += '\nAccording to [Maven parallel test](https://www.baeldung.com/maven-junit-parallel-tests), we can run tests in parallel.\n'
            }
        }
        description += '\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n'
        return [title, description, outFilePaths]
    }

    static StateFlag useMavenStrategy(Repository repository, String repoPath, String originRepoName, MavenStrategy strategy) {
        String contentFilePath = Util.getPullRequestListFilePath(strategy)
        //已经发过该strategy的项目就不发了
        File contentFile = new File(contentFilePath)
        String content = contentFile.exists() ? contentFile.text : ""
        if (content.contains(originRepoName) && originRepoName != 'ChenZhangg/CILink')
            return null
        return MavenChecker.check(repository,repoPath,strategy)
    }

    static Boolean applyFix(String repoPath, String originRepoName, MavenStrategy strategy, boolean isExplicit){
        if(isExplicit){
            MavenExplicitSmellFixer fixer = new MavenExplicitSmellFixer(repoPath,strategy)
            return fixer.fixer()
        }
        else{
            // 隐式引入
            MavenCategory category = getMavenCategory(strategy)
//            if (strategy == MavenStrategy.MAVEN_PARALLEL_EXECUTION) {
//                ParallelBuildFixer.parallelExecutionFixer(repoPath, originRepoName)
//            } else if (strategy == MavenStrategy.MAVEN_UNUSED_DEPENDENCY) {
//                DependencyFixer.unusedDependeniesFixer(repoPath, originRepoName)
//            } else
            if (category == MavenCategory.TEST || category == MavenCategory.FORK) {
                return TestFixer.fixer(repoPath, strategy)
            } else if (category == MavenCategory.COMPILATION) {
                return CompilationFixer.fixer(repoPath, strategy)
            }
        }
        return true
    }

    static List<MavenStrategy> getMavenStrategies(Repository repository, String repoPath, String originRepoName, MavenCategory category, boolean isExplicit) {
        List<MavenStrategy> strategies = []
        for (MavenStrategy strategy : strategiesOfCategory.get(category)) {
            def flag = useMavenStrategy(repository, repoPath, originRepoName, strategy)
            println("${strategy.toString()}:${flag.toString()}")
            if (isExplicit && flag == StateFlag.CLOSE) {
                strategies << strategy
            }else if(!isExplicit && flag ==StateFlag.DEFAULT){
                strategies << strategy
            }
        }
        return strategies
    }


    static Boolean createMavenPullRequest(MavenCategory category, Repository repository, boolean isExplicit){
        String repoName = repository.repoName
        println("开始处理"+category.toString()+"策略的项目${repository.id}: ${repoName}")
        try {
           return run(repository,category,isExplicit)
        } catch (Exception e) {
            e.printStackTrace()
        }
        println("处理完"+category.toString()+"策略的项目${repository.id}: ${repoName}")
    }

    static Map<MavenCategory, List<Repository>> getRepositoriesMap(){
        //数据库中Maven项目有2547个，以500为一组划分
        //初始的id范围(678，127772] 、(127772,354695]、（354695，769560]、（769560,1768693]、(1768693,6624504]
        Map<MavenCategory, List<Repository>> repositoriesMap = [(MavenCategory.TEST)       : []]
//                                                                (MavenCategory.TEST)       : [],]
        for (Repository repository : MysqlUtil.getRepositories()) {
            if(repository.buildTool == 1){
                if(repository.id > 125603 && repository.id <= 127772 ){
//                    //MAVEN_COMPILER_DAEMON作用于1000个以上源文件的项目
//                    if(repository.getJavaFilesNum()>=1000){
//                        repositoriesMap.get(MavenCategory.COMPILATION).add(repository)
//                    }
                }else if(repository.id > 176275 && repository.id <= 354695){
                    repositoriesMap.get(MavenCategory.TEST).add(repository)
//                    if(repository.getJavaFilesNum()>=1000 && repository.id>265192){
//                        repositoriesMap.get(MavenCategory.COMPILATION).add(repository)
//                    }else{
//                        repositoriesMap.get(MavenCategory.TEST).add(repository)
//                    }
                }else if(repository.id > 442934 && repository.id <= 769560){
//                    repositoriesMap.get(MavenCategory.FORK).add(repository)
//                    if(repository.getJavaFilesNum()>=1000 && repository.id>376290){
//                        repositoriesMap.get(MavenCategory.COMPILATION).add(repository)
//                    }else if(repository.id > 379320) {
//                        repositoriesMap.get(MavenCategory.FORK).add(repository)
//                    }
                }
            }
        }
        repositoriesMap.each { MavenCategory category, List<Repository> repositories ->
            println(category.toString() + "长度为：" + repositories.size())
        }
        return repositoriesMap
    }

    static Map<MavenStrategy, List<Repository>> getExplicitRepositoriesMap(){
        Map<MavenStrategy, List<Repository>> repositoriesMap = [
//                (MavenStrategy.MAVEN_PARALLEL_TEST)  : [],
//                (MavenStrategy.MAVEN_REPORT_GENERATION)  : [],
                (MavenStrategy.MAVEN_FORK_TEST)  : [],
        ]

        for (Repository repository : MysqlUtil.getRepositories()) {
//            if(repository.id>403272 && repository.buildTool==1 && repository.parallelTest==0){
//                repositoriesMap.get(MavenStrategy.MAVEN_PARALLEL_TEST) << repository
//            }
//            if(repository.buildTool==1 && repository.mavenReportGeneration==0){
//                repositoriesMap.get(MavenStrategy.MAVEN_REPORT_GENERATION) << repository
//            }
            if (repository.buildTool==1 && repository.mavenForkTest==0){
                repositoriesMap.get(MavenStrategy.MAVEN_FORK_TEST) << repository
            }
        }
        repositoriesMap.each{strategy,repositories->
            println("${strategy.toString()}显式引入的repositories长度为: " + repositories.size())
        }
        return repositoriesMap
    }

    static void createMavenPullRequest(boolean isExplicit=false) {
        if(isExplicit){
            def repositoriesMap = getExplicitRepositoriesMap()
            int count = 0
            def error = []
            repositoriesMap.each { strategy,repositories ->
                println("开始处理smell:" + strategy.toString()+"\n")
                repositories.each {repository->
                    println(repository.repoName)
                    try {
                        def PRCreated =  run(repository,getMavenCategory(strategy),isExplicit,strategy)
                        if(PRCreated){
                            count++
                            goSleep()
                        }
                    } catch (Exception e) {
                        error << "${strategy.toString()} : ${repository.repoName}"
                        e.printStackTrace()
                    }
                    if (count==50) {
                        println("处理MAVEN_FORK_TEST策略的最后一个项目的ID为：" + repository.id +"\n")
                    }
                }
            }
            if(error.size()!=0){
                println("\n==================\n error:")
                error.each {println(it)}
            }
        }else{
            Map<MavenCategory, List<Repository>> repositoriesMap = getRepositoriesMap()
            int count = 100 //每种策略一次性处理的项目数量
            def reportRepo = []
            repositoriesMap.each { MavenCategory category, List<Repository> repositories ->
                for (int i = 0; i < count && reportRepo.size()<=9; i++) {
                    def PRCreated = createMavenPullRequest(category, repositories[i],isExplicit)
                    if(PRCreated){
                        reportRepo << repositories[i].repoName
                        goSleep()
                    }
                    if (i == count - 1) {
                        println("处理" + category.toString() + "策略的最后一个项目的ID为：" + repositories[i].id +"\n\n")
                    }
                }
            }
        }
    }

    static void goSleep(){
        Random random = new Random()
        long sleepTime = 15 + random.nextInt(15)
        println("休息"+sleepTime+"分钟")
        long start = System.currentTimeMillis()
        sleep(sleepTime*60000)     //1min=60000ms
        long end = System.currentTimeMillis()
        println("休息结束，实际休息时间："+(end-start)/60000 + "min")
    }


    static void main(String[] args) {
        // createMavenPullRequest(true)
        String repoPath = args[0];
        String originRepoName = args[1];

        List<MavenStrategy> strategies = [MavenStrategy.MAVEN_PARALLEL_EXECUTION, MavenStrategy.MAVEN_FORK_TEST, MavenStrategy.MAVEN_REPORT_GENERATION, MavenStrategy.MAVEN_COMPILER_DAEMON,MavenStrategy.MAVEN_PARALLEL_TEST]

        println("repoPath: " + repoPath)
        println("originRepoName: " + originRepoName)

        for (MavenStrategy strategy : strategies) {
            applyFix(repoPath, originRepoName, strategy, false)
            applyFix(repoPath, originRepoName, strategy, true)

        }
    }
}
