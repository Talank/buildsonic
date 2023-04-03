import model.Repository
import smell.StateFlag
import smell.TravisFixer
import smell.checker.TravisChecker
import smell.checker.gradle.GradleChecker
import smell.checker.maven.MavenChecker
import util.*

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

import static util.GradleUtil.strategiesOfCategory
import static util.MavenUtil.*
import static util.GradleUtil.*
import static util.TravisUtil.*

/**
 * 创建一个固定Github仓库:86 zc/Performance，用于触发Travis CI
 * 项目的PR状态：merged / notMerged
 * merged：../sequence/fork中到找相应的仓库，切换到默认分支
 * notMerged: 解压对应的codeZIP，将其中的文件复制到TravisTrigger仓库
 *
 */
class TravisTrigger {
    /**
     * 新的触发程序：每一个smell建立一个分支，分支命名格式 "项目名-pull编号-smell/origin"
     * 开启四个GitHub Repo :GradleProperty、 GradleBuild、 MavenTest 、 MavenCompile
     * 每个smell都需要两个build数据：origin  和 fix
     * 0.切换到master分支：只保留README.md
     * 1.首先判断："项目名-pull编号-origin"分支是否存在，如果存在就不需要再次构建,并切换到该分支
     *          如果不存在，就在master分支基础上创建新分支，然后清空代码，copy对应内容到本触发Repo构建
     * 2.在"项目名-pull编号-origin"分支基础上创建新分支"项目名-pull编号-smell"，并切换
     * 3.在"项目名-pull编号-smell"分支上修复smell
     * 4.add->commit->push -u origin "项目名-pull编号-smell"
     *
     * @param repoInfo
     * @param strategy
     * @param isMerged
     */
    static void run(String repoInfo, Object strategy, Boolean isMerged) {
        def (originRepoName, pulls) = TriggerUtil.urlToRepo(repoInfo)  // owner/repo + pulls
        println("处理${strategy.toString()}的项目: ${originRepoName}")
        def triggerRepoPath = Util.getTriggerRepoPath(strategy)
        if(!new File(triggerRepoPath).exists()){
            println("trigger仓库未下载:${triggerRepoPath}")
            return
        }

        // 0.切换到master分支
//        GitUtil.CheckoutAndCreate(triggerRepoPath,"master",null)


        // 1.判断"项目名-pull编号-origin"分支是否存在
        String originBranch = "${originRepoName}-${pulls}-origin"
        def originBranchExit =  GitUtil.CheckoutAndCreate(triggerRepoPath,originBranch,"master")
        if (originBranchExit==null){
            println("切换到origin branch时出错")
            return
        }
        if (!originBranchExit){
            // 如果origin分支不存在，则在origin/master基础上创建新分支
            println("origin分支不存在,建立新分支")
            boolean init = TriggerUtil.initTravisTriggerRepo(triggerRepoPath,repoInfo,originRepoName,isMerged)
            if(!init){
                println("${originRepoName}初始化失败")
                return
            }
            sleep(1000*5)
            TravisFixer travisFixer = new TravisFixer(Paths.get(triggerRepoPath,'.travis.yml').normalize().toString())
            travisFixer.updateBranches(originBranch)
            sleep(1000*5)
            String commitMessageOfOrigin = "${originRepoName}:original code"
            GitUtil.addAndCommit(triggerRepoPath, commitMessageOfOrigin)
            HubUtil.push(triggerRepoPath, originBranch)  // git push -u origin branchName
            sleep(1000*60)
        }else{
            // 如果origin分支存在，则跳过init部分
            println("origin分支存在")
            sleep(1000*5)
        }

        // 2.当前处于origin分支上，需要在此基础上创建新分支"项目名-pull编号-smell"，并切换
        String smellBranch = "${originRepoName}-${pulls}-${strategy.toString()}"
        GitUtil.CheckoutAndCreate(triggerRepoPath,smellBranch,originBranch)

        // 3.在"项目名-pull编号-smell"分支上修复smell，触发Repo构建

        if(strategy instanceof TravisStrategy){
            Repository repository = MysqlUtil.getRepositoryByName(originRepoName)
            String ymlFilePath = Paths.get(triggerRepoPath,'.travis.yml').normalize().toString()
            TravisChecker checker = new TravisChecker(ymlFilePath)
            def flag = checker.check((TravisStrategy)strategy)
            def strategyWithFlag = [((TravisStrategy)strategy):flag]
            PullRequestTravisCreator.applyFix(triggerRepoPath,repository,strategyWithFlag)

        } else if(strategy instanceof GradleStrategy){
            GradleCategory category = getGradleCategory((GradleStrategy)strategy)
            List<GradleStrategy> strategies = [(GradleStrategy)strategy]
            def flag = GradleChecker.check(triggerRepoPath,originRepoName,(GradleStrategy)strategy)
            if(flag== StateFlag.CLOSE){
                PullRequestGradleCreator.applyFix(triggerRepoPath,originRepoName,strategies, category,true)
            }else if(flag== StateFlag.DEFAULT){
                PullRequestGradleCreator.applyFix(triggerRepoPath, originRepoName,strategies, category,false)
            }

        } else if(strategy instanceof MavenStrategy){
            def flag = MavenChecker.check(triggerRepoPath,originRepoName,(MavenStrategy)strategy)
            if(flag== StateFlag.CLOSE){
                PullRequestMavenCreator.applyFix(triggerRepoPath, originRepoName,(MavenStrategy)strategy,true)
            }else if(flag== StateFlag.DEFAULT){
                PullRequestMavenCreator.applyFix(triggerRepoPath, originRepoName,(MavenStrategy)strategy,false)
            }
        }

        // 4.将修复后的code push到github，触发travis ci
        sleep(1000*5)
        TravisFixer travisFixer = new TravisFixer(Paths.get(triggerRepoPath,'.travis.yml').normalize().toString())
        travisFixer.updateBranches(smellBranch)
        sleep(1000*5)
        String commitMessageOfStrategy = "${originRepoName}:fixed ${strategy.toString()}"
        GitUtil.addAndCommit(triggerRepoPath, commitMessageOfStrategy)
        HubUtil.push(triggerRepoPath, smellBranch)
    }

    static Integer getGitDepth(String repoInfo, boolean isMerged){
        String originRepoPath = isMerged?TriggerUtil.initTriggerIsMerged(repoInfo):TriggerUtil.initTriggerNotMerged(repoInfo)
        String ymlFilePath = Paths.get(originRepoPath, ".travis.yml").normalize().toString()
        println(originRepoPath)
        if(!new File(ymlFilePath).exists()){
//            println("yml不存在")
            return null
        }
        TravisChecker checker = new TravisChecker(ymlFilePath)
        def depth = checker.shallowCloneValue()
        if(depth==50){
            return 49
        }
        if(depth==null || depth==-1){
            return 50
        }
        return depth
    }

    static Double getCloneTime(String repoName, int depth){
        String repoPah = Paths.get(Util.gitDepthTestPath,repoName.split('/')[1]).normalize().toString()
        if(new File(repoPah).exists()){
            println("存在相同仓库，先删除")
            def rmRepo = "rm -rf ${repoPah}".execute()
            rmRepo.waitFor()
            println("删除成功")
        }
        def cloneCommand = "time git clone -q --depth ${depth} git@github.com:${repoName}.git"
        def cloneExecute = cloneCommand.execute(null,new File(Util.gitDepthTestPath))
        def out = new StringBuffer()
        def error = new StringBuffer()
        cloneExecute.waitForProcessOutput(out,error)
        Pattern pattern = ~/(.*)system(.*)elapsed(.*)/
        Matcher matcher = pattern.matcher(error.toString().trim())
        if(matcher.find()){
            String elapsed = matcher.group(2)   // 0:22.38
            double cloneTime = (elapsed.split(':')[0].toInteger())*60 + elapsed.split(':')[1].toDouble()
            return cloneTime
        }
        return null
    }

    static void shallowCloneTrigger(List<String> reposInfo){
        Map<String,List<Double>> repoCloneInfos = new HashMap<>()
        reposInfo.each {repoInfo->
            boolean isMerged = repoInfo.startsWith("https://github.com")
            String originRepoName = isMerged?TriggerUtil.urlToRepo(repoInfo)[0]:repoInfo   // owner/repo
            def depth = getGitDepth(repoInfo,isMerged)
//            println("${originRepoName}的depth:${depth}")
            if(depth < 50){
                // git depth小于50认为是显式消除
//                println("${repoInfo} git:depth<=50")
                return true
            }
            def originalCloneTime = getCloneTime(originRepoName,depth)
            def fixedCloneTime = getCloneTime(originRepoName,3)
            repoCloneInfos.put(originRepoName,[originalCloneTime,fixedCloneTime])
        }
        repoCloneInfos.each {repoName,cloneTimes->
            println(repoName+":"+cloneTimes[0]+"  "+cloneTimes[1])
        }
    }


    static void createTrigger(Object strategy){
//        def triggeredRepos = TravisAPIUtil.getTriggeredRepo(strategy.toString())
        def (notMergedPR, mergedPR) = TriggerUtil.getPRUrlBySmell(strategy.toString())
        println("notMergedRepo size : "+notMergedPR.size())
        println("mergedRepo size : "+mergedPR.size())

        if (strategy==GradleStrategy.GRADLE_FORK_TEST){
            notMergedPR = notMergedPR.subList(16,notMergedPR.size())
        }

        for(String PRUrl : notMergedPR){
            if(PRUrl.contains("openmrs/openmrs-core")){
                continue
            }
            try{
                run(PRUrl, strategy, false)   // PRUrl: https://github.com/{owner/repo}/pull/{number}
                println("${strategy}项目处理结束\n")
            }catch (Exception e){
                e.printStackTrace()
            }
            sleep(1000*60)
        }
        for(String PRUrl : mergedPR){
            try{
                run(PRUrl, strategy, false)   // PRUrl: https://github.com/{owner/repo}/pull/{number}
                println("${strategy}项目处理结束\n")
            }catch (Exception e){
                e.printStackTrace()
            }
            sleep(1000*60)
        }
    }

    static void getAndSavaData(List<Object> strategies){
        strategies.each {strategy->
            def buildInfo = TravisAPIUtil.getAndSaveBuildsInfo(strategy.toString())
            TravisAPIUtil.parserBuildsInfo(strategy.toString(),buildInfo)
        }
    }

    static void travisSmellTrigger(){
//        def strategies = [TravisStrategy.TRAVIS_FAST_FINISH,TravisStrategy.TRAVIS_CACHE]
//        strategies.each {strategy->
//            createTrigger(strategy)
//        }
        println("TRAVIS_FAST_FINISH开始处理")
        createTrigger(TravisStrategy.TRAVIS_FAST_FINISH)
    }

    static void threadRun(){
//        TriggerUtil.preparation()

//        def gradleBuilds = [
//                GradleStrategy.GRADLE_COMPILER_DAEMON,
//                GradleStrategy.GRADLE_INCREMENTAL_COMPILATION,
//                GradleStrategy.GRADLE_FORK_TEST,
//                GradleStrategy.GRADLE_REPORT_GENERATION,
//        ]
//        def gradleProperties = [
//                                GradleStrategy.FILE_SYSTEM_WATCHING,
//                                GradleStrategy.CONFIGURATION_ON_DEMAND,
//                                GradleStrategy.PARALLEL_BUILDS,
//                                GradleStrategy.GRADLE_DAEMON]
//        def smells = [gradleBuilds,gradleProperties]

        // 84服务器跑maven的
        def mavenTest = [MavenStrategy.MAVEN_PARALLEL_TEST,MavenStrategy.MAVEN_REPORT_GENERATION,MavenStrategy.MAVEN_FORK_TEST]
        def mavenCompile = [ MavenStrategy.MAVEN_PARALLEL_EXECUTION,MavenStrategy.MAVEN_COMPILER_DAEMON]
        def smells = [mavenTest, mavenCompile]

//        gradleBuilds.each {strategy->
//            println(" \n开始处理smell ：" + strategy.toString())
//            createTrigger(strategy)
//        }

        smells.each {strategies->
            Thread.start {
                println( "线程启动" + Thread.currentThread().getName())
                strategies.each {strategy->
                    println(" \n开始处理smell ：" + strategy.toString())
                    createTrigger(strategy)
                }
            }
        }
    }

    static void main(String[] args) {
//        threadRun()
    }
}
