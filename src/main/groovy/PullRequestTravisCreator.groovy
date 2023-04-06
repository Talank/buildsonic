import model.Repository
import smell.StateFlag
import smell.checker.TravisChecker
import smell.TravisFixer
import util.GitUtil
import util.GithubUtil
import util.HubUtil
import util.MavenUtil
import util.MysqlUtil
import util.TriggerUtil
import util.Util
import java.nio.file.Files
import java.nio.file.Paths
import util.TravisUtil.TravisStrategy
class PullRequestTravisCreator {


    /**
     *
     * @param repository
     * @param strategy: 默认为Null,如果不传入strategy，就认为是检测该repository的所有travis smell
     */
    static Boolean run(Repository repository, TravisStrategy strategy = null) {
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
        String branchName = GitUtil.createAndCheckoutBranch(repoPath, "Modify_Travis", defaultBranchName)

        //获取要执行的修复策略
        String ymlFilePath = Paths.get(repoPath, ".travis.yml").normalize().toString()
        if(!new File(ymlFilePath).exists()){
            println(".travis.yml不存在")
            return false
        }
        return true
        def strategyWithFlag= getTravisStrategies(repository,repoPath, originRepoName,strategy)
        if (strategyWithFlag.size()==0) {
            println("未检测到cache smell")
            return false
        }

        //进行修复操作 写入到文件中 并提交commit
        applyFix(repoPath,repository,strategyWithFlag)
        String commitMessage = 'Improve Travis CI build Performance'

//        if (strategyCombine == StrategyCombine.TRAVIS) {
//            commitMessage = 'Improve Travis CI build Performance'
//        }
        GitUtil.addAndCommit(repoPath, commitMessage)

        //push
        HubUtil.push(repoPath, branchName)

        //发起pull request
        String head = "${GithubUtil.USER_NAME}:${branchName}"
        def (title, description, outFilePaths) = getDescription(strategyWithFlag.keySet().toList())
        return GithubUtil.pullRequest(originRepoName, head, defaultBranchName, title, description, outFilePaths)
    }

    static String getPRTitle(TravisStrategy strategy){
        def cachingTitles = [
                'How about turning on the Travis CI Cache feature',
                'Enable caching for Travis CI build',
                'The cache of Travis CI can improve build performance',
                'Build times can be reduced by cache in Travis',
        ]
        int randomNum = new Random().nextInt(4)
        if(strategy== TravisStrategy.TRAVIS_CACHE){
            return cachingTitles[randomNum]
        }
        return ""
    }
    //[Caching Dependencies and Directories](https://docs.travis-ci.com/user/caching/) Travis CI can cache content that does not often change, to speed up the build process.

    static List<String> getDescription(List<TravisStrategy> strategies) {
        String title = getPRTitle(strategies[0])
        title = title==""?'Improve Travis CI build Performance':title
        String description = ""
        List<String> outFilePaths = []
        for (TravisStrategy strategy : strategies) {
            outFilePaths << Util.getPullRequestListFilePath(strategy)
            if (strategy == TravisStrategy.TRAVIS_CACHE) {
                description += "\n[Caching Dependencies and Directories](https://docs.travis-ci.com/user/caching/) Travis CI can cache content that does not often change, to speed up the build process.\n"
            } else if (strategy == TravisStrategy.TRAVIS_SHALLOW_CLONE) {
                description += '\nAccording to [git-clone-depth](https://docs.travis-ci.com/user/customizing-the-build#git-clone-depth), Travis CI provide a way to shallow clone a repository. This has the obvious benefit of speed, since you only need to download a small number of commits.\n'
            } else if (strategy == TravisStrategy.TRAVIS_RETRY) {
                description += '\nDoes travis_retry really solve the build issues? According to the data in paper [An empirical study of the long duration of continuous integration builds](https://dl.acm.org/doi/10.1007/s10664-019-09695-9), travis_retry can only solve 3% of the build failures. And it may cause unstable build and increase build time.\n'
            } else if (strategy == TravisStrategy.TRAVIS_WAIT) {
                description += '\nAccording to [Build times out because no output was received](https://docs.travis-ci.com/user/common-build-problems/#build-times-out-because-no-output-was-received), we should carefully use travis_wait, as it may make the build unstable and extend the build time.\n'
            } else if (strategy == TravisStrategy.TRAVIS_FAST_FINISH) {
                description += '\nAccording to the official document [Fast Finishing](https://docs.travis-ci.com/user/build-matrix/#fast-finishing), if some rows in the build matrix are allowed to fail, we can add fast_finish: true to the .travis.yml to get faster feedbacks.\n'
            }
        }
        description += '\n=====================\nIf there are any inappropriate modifications in this PR, please give me a reply and I will change them.\n'
        return [title, description, outFilePaths]
    }

    static StateFlag useTravisStrategy(String originRepoName, String ymlFilePath, TravisStrategy strategy) {
        String contentFilePath = Util.getPullRequestListFilePath(strategy)
        File contentFile = new File(contentFilePath)
        String content = contentFile.exists() ? contentFile.text : ""
        if (content.contains(originRepoName) && originRepoName != 'ChenZhangg/CILink')
            return null

        TravisChecker checker = new TravisChecker(ymlFilePath)
        return checker.check(strategy)
    }

    static Map<TravisStrategy, StateFlag> getTravisStrategies(Repository repository,String repoPath, String originRepoName,TravisStrategy strategy) {
        Map<TravisStrategy,StateFlag> strategyWithFlag = new HashMap<>()
        String ymlFilePath = Paths.get(repoPath, ".travis.yml").normalize().toString()
        def strategies = strategy==null? TravisStrategy.values():[strategy]
        strategies.each{ TravisStrategy travisStrategy->
            def flag = useTravisStrategy(originRepoName, ymlFilePath, travisStrategy)
            if(flag!=null){
                strategyWithFlag.put(strategy,flag)
            }
        }
        return strategyWithFlag
    }

    static void applyFix(String repoPath, Repository repository, Map<TravisStrategy,StateFlag> strategyWithFlag) {
        String travisFilePath = Paths.get(repoPath, ".travis.yml")
        File travisFile = new File(travisFilePath)
        if (!travisFile.exists()) {
            throw new Exception("不存在文件${travisFilePath}}")
        }
        TravisFixer fixer = new TravisFixer(travisFilePath)
        fixer.travisSmellFixer(strategyWithFlag,repository.buildTool)
    }

    static List<Repository> getTestTimeRepositories(){
        List<Repository> repositories = MysqlUtil.getRepositories()
        def cacheRepositories = []
        for(Repository repository :repositories ){
            if(repository.travisCache!=null && !repository.travisCache){
                cacheRepositories << repository
            }
        }
        return cacheRepositories
    }

    static Boolean createTravisPullRequest(Repository repository,TravisStrategy strategy){
        println("开始处理项目${repository.id}: ${repository.repoName}")
        try {
            return run(repository,strategy)
        } catch (Exception e) {
            e.printStackTrace()
        }
        println("处理完项目${repository.id}: ${repository.repoName}")
        return null
    }

    static void createTravisPullRequest() {
        File file = new File(Util.TRAVIS_CACHE_LIST_PATH)
        String cacheContent = file.exists() ? file.text : ""
        List<Repository> repositories = MysqlUtil.getRepositories()
        List<String> spRepoNames = ['oshi/oshi', 'java-native-access/jna']
        for (Repository repository : repositories) {
            String repoName = repository.repoName
            //提交过shall clone的项目暂时不发pull request 后续会删除该语句
            //if (shallowCloneContent.contains(repoName))
            //    continue

            if (repository.id <= 83541) {//4945
                continue
            }
            if (cacheContent.contains(repoName) || spRepoNames.contains(repoName))
                continue

            if (/*repository.getTravisGitDepth() == null || repository.getTravisRetry() || repository.getTravisWait() || */repository.getTravisCache() == null /*|| (repository.getTravisAllowFailures() && repository.getTravisFastFinish() == null)*/) {
                println("开始处理项目${repository.id}: ${repoName}")
                try {
                    run(repository, null)
                } catch (Exception e){
                    e.printStackTrace()
                }
                println("处理完项目${repository.id}: ${repoName}")
            }
        }
    }

    static void goSleep(){
        Random random = new Random()
        long sleepTime = 10+ random.nextInt(10)
        println("休息"+sleepTime+"分钟")
        long start = System.currentTimeMillis()
        sleep(sleepTime*60000)     //1min=60000ms
        long end = System.currentTimeMillis()
        println("休息结束，实际休息时间："+(end-start)/60000 + "min")
    }

    static void bsTravisReplica(String[] args){
        String repoPath = args[0];

        Map<TravisStrategy,StateFlag> strategyWithFlag = new HashMap<>()
        def strategies = [TravisStrategy.TRAVIS_SHALLOW_CLONE, TravisStrategy.TRAVIS_RETRY, TravisStrategy.TRAVIS_WAIT, TravisStrategy.TRAVIS_CACHE,TravisStrategy.TRAVIS_FAST_FINISH]
        String ymlFilePath = Paths.get(repoPath, ".travis.yml").normalize().toString()   
        strategies.each{ TravisStrategy travisStrategy->
            TravisChecker checker = new TravisChecker(ymlFilePath)     
            def flag = checker.check(travisStrategy)
            if(flag!=null){
                strategyWithFlag.put(travisStrategy,flag)
            }
        }

        String gradleFile = Paths.get(repoPath, "build.gradle").normalize().toString()
        def buildTool = 1
        if(new File(gradleFile).exists()){
            buildTool = 2;
        }

        TravisFixer fixer = new TravisFixer(ymlFilePath)
        fixer.travisSmellFixer(strategyWithFlag,buildTool)
    }

    static void main(String[] args) {
        //Travis的smell中 cache、fast finish、shallow clone分显式引入和隐式引入
        // wait和retry都只有显式引入
        //createTravisPullRequest()
        bsTravisReplica(args)
    }
}

