package util

import org.junit.runners.model.MultipleFailureException
import parser.YmlParser
import smell.TravisFixer

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import groovy.json.JsonSlurper
import model.Repository
import util.*

class TriggerUtil {


    /**
     * 根据传入的repoName下载该项目默认分支上最新的code(ZIP形式，不包含.git）
     * @param repoName： owner/repo
     * @return Boolean,如果下载失败返回false
     */
    static Boolean downloadZip(String repoName){
        String zipPath =Paths.get(Util.codeZipPath,"${repoName.replace('/','@')}.zip").normalize().toString()
        if(new File(zipPath).exists()){
            return true
        }
        def defaultBranchName = GithubUtil.getDefaultBranchName(repoName)
        def downloadZipCommand = "curl -L https://api.github.com/repos/${repoName}/zipball/${defaultBranchName}  --output ${zipPath}"
        def downloadZipExecute = downloadZipCommand.execute()
        def out = new StringBuffer()
        def error = new StringBuffer()
        downloadZipExecute.waitForProcessOutput(out,error)
        Closure checkZipDownload ={
            int retries = 0
            while(retries++ < 4){
                def checkZipCRC = "unzip -tq ${zipPath}".execute()  // 检查压缩文件是否正确
                def outputBuffer = new StringBuffer('')
                checkZipCRC.consumeProcessOutputStream(outputBuffer)
                if (outputBuffer.toString().startsWith("No errors") ||outputBuffer.size()==0){
                    println("${repoName.replace('/','@')}.zip下载成功")
                    return true
                }else{
                    //如果检测出错，就重新下载
                    downloadZipExecute = downloadZipCommand.execute()
                    downloadZipExecute.waitForProcessOutput(out,error)
                    println("第${retries}次下载出错，重新下载${repoName.replace('/','@')}.zip")
                }
            }
            return false
        }
        if (!checkZipDownload.call()){
            println("${repoName.replace('/','@')}.zip下载失败")
            println(error.toString())
            return false
        }
        return true
    }

    /**
     * 解压repo对应项目的code.zip，zip和解压后的文件夹位于Util.codeUnzipPath路径下
     * @param repoName： owner/repo
     * @return 解压失败，返回null；否则返回解压后文件的绝对路径
     */
    static String unzipCode(String repoName){
        String zipPath =Paths.get(Util.codeZipPath,"${repoName.replace('/','@')}.zip").normalize().toString()
        if (!new File(zipPath).exists()){
            println("${zipPath}不存在,重新下载")
            if(!downloadZip(repoName)){
                println("${zipPath}重新下载失败")
                return false
            }
        }
        println(zipPath)
        def unzipCommand = "unzip -o -d ${Util.codeUnzipPath}  ${zipPath}"
        def unzipExecute = unzipCommand.execute()
        def out = new StringBuffer()
        def error = new StringBuffer()
        unzipExecute.waitForProcessOutput(out,error)
        def find = new File(Util.codeUnzipPath).listFiles().find() {file->
            // code.zip解压后的文件名默认为: owner-repo-commitHashNumber
            if(file.getName().contains(repoName.replace('/','-'))){
                println("${zipPath}解压成功")
                return true
            }
        }
        if(find==null || error.size()>0){
            println("${zipPath}解压失败")
            println(error.toString())
            return null
        }
        return find.absolutePath
    }

    /**
     * 将sourceDirectory的除了.git以外的所有文件（包含隐藏文件），全部复制到targetDirectory中
     * @param targetDirectory:绝对路径
     * @param sourceDirectory:绝对路径
     * @return：如果command执行结果的error内容不为空，返回false，否则返回true
     */
    static Boolean copyAllFilesToAnotherDirectory(String targetDirectory, String sourceDirectory){
        def copyCommand = "rsync -a --exclude .git ${sourceDirectory}/. ${targetDirectory}"
        def copyExecute = copyCommand.execute()
        def out = new StringBuffer()
        def error = new StringBuffer()
        copyExecute.waitForProcessOutput(out,error)
        if (error.size()>0){
            println(error.toString())
            return false
        }
        sleep(1000*30)
        return true
    }

    /**
     * 清空该目录下的所有文件，除了.git
     * @param GitDirectory:绝对路径
     * @return
     */
    static Boolean emptyGitDirectory(String triggerRepoPath){
        GitUtil.deleteAllFiles(triggerRepoPath)
        return new File(triggerRepoPath).listFiles().size() == 1
    }

    /**
     * 根据smell，返回该smell对应的sheet数据：分别是没有merge和merged的repos(没有附带测试时间的）
     * @param smell：smell和sheet名对应
     * @return [notMergedPR[] , MergedPR[] ]
     */
    static List<List<String>> getPRUrlBySmell(String smell){
        ExcelUtil excel = new ExcelUtil(Util.PRDetailFilteredPath, smell)
        def notMergedRepo = excel.getCellsConditionally([0:"是",3:"FALSE",6:"隐式引入",10:""],1)
        def mergedRepo = excel.getCellsConditionally([0:"是",3:"TRUE",6:"隐式引入",10:""],1)
//        notMergedRepo = urlToRepo(notMergedRepo)  // owner/repo
//        mergedRepo = urlToRepo(mergedRepo)      // https://github.com/{owner/repo}/pull/{number}
        return [notMergedRepo, mergedRepo]
    }

    static List<String> urlToRepo(List<String> urls){
        List<String> repos = new ArrayList<>()
        urls.each{
            def (repo, pulls) = urlToRepo(it)
            repos << repo
        }
        return repos
    }

    static List<String> urlToRepo(String urls){
        Pattern pattern = ~/https:\/\/github.com\/(.*)\/pull\/(.*)/
        Matcher matcher = pattern.matcher(urls)
        if(matcher.find()){
            return [matcher.group(1), matcher.group(2)]
        }
        return [null, null]
    }

    /**
     * @return 返回所有发过PR的项目 List<String> repos , repo形式:owner/repo
     */
    static List<String> getAllRepos(){
        List<String> repos = new ArrayList<>()
        ExcelUtil excel = new ExcelUtil(Util.PRDetailFilteredPath)
        def strategies = []
        strategies << MavenUtil.MavenStrategy.MAVEN_PARALLEL_TEST.toString()
        strategies << MavenUtil.MavenStrategy.MAVEN_PARALLEL_EXECUTION.toString()
        strategies << MavenUtil.MavenStrategy.MAVEN_FORK_TEST.toString()
        strategies << MavenUtil.MavenStrategy.MAVEN_REPORT_GENERATION.toString()
        strategies << MavenUtil.MavenStrategy.MAVEN_COMPILER_DAEMON.toString()
        def sheets = excel.getSheets()
        sheets.each {
            if(strategies.contains(it.sheetName)){
                excel.setSheet(it)
                def cells = excel.getCells(0)
                repos += cells
            }
        }
        repos = urlToRepo(repos)
        return repos.unique()
    }

    static List<String> downloads(List<String> repoNames){
        List<String> downloadError = new ArrayList<>()
        repoNames.each {
            if (!downloadZip(it)){
                downloadError << it
            }
        }
        return downloadError
    }

    /**
     * 根据repoUrl得到../sequence/fork里面对应repo的绝对路径
     * 由于一些repo，不同的账号可能对其发过多个不同类型的PR
     * 所以在fork里面，存在这种情况：有两个子目录——[账号1@某repo, 账号2@某repo]
     * 所以先要通过./resources/pullRequest@json中的json文件，读取发PR的账号
     * @param repoUrl: PR的url
     * @return forkRepoPath: 绝对路径
     */
    static String getForkRepoPath(String repo, String pulls){
        Closure downloadGithubAPI = { String jsonPath ->
            def token = "token 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"
            def host = "https://api.github.com/repos/${repo}/pulls/${pulls}"
           // def savePath = "./resources/pullRequest@json/${jsonName}"
//        def command = "curl -H \"User-Agent:Mozilla/5.0\" -H \"Authorization: ${token} \" -H \"Accept:application/json\" \"${host}\" | sed 's/,/\\n/g'  | grep \"login\" | head -1"
            def command = "curl -H \"User-Agent:Mozilla/5.0\" -H \"Authorization: ${token} \" -H \"Accept:application/json\" \"${host}\" > ${jsonPath}"
            try{
                def process = [ 'bash', '-c', command ].execute()
                process.waitFor()
                return true
            } catch (Exception e){
                e.printStackTrace()
                return false
            }
        }
        Closure parseJson = { String jsonPath->
            if(!new File(jsonPath).exists()){
                return null
            }
            def jsonSlurper = new JsonSlurper()
            def repoJson = jsonSlurper.parse(new File(jsonPath))
            def prCreator = repoJson['user']['login']
            return  Paths.get(Util.forkDirectoryPath,"${prCreator}@${repo.split('/')[1]}").normalize().toString()
        }

        String repoJsonName = "${repo.replace('/','@')}@${pulls}.json"   //例如：ical4j@ical4j@535.json(新的json文件命名方式）
        String repoJsonPath = Paths.get(Util.pullRequestJson, repoJsonName).normalize().toString()
        if(!new File(repoJsonPath).exists()){
            // 如果没有下载Github API数据，尝试下载
           downloadGithubAPI.call(repoJsonPath)
        }

        def forkRepoPath = parseJson.call(repoJsonPath)
        if (forkRepoPath!=null){
            return forkRepoPath
        }

        repoJsonName = "${repo.replace('/','@')}@pulls@${pulls}.json"   //例如：ical4j@ical4j@pulls@535.json(旧的json文件命名方式）
        repoJsonPath = Paths.get(Util.pullRequestJson, repoJsonName).normalize().toString()
        forkRepoPath = parseJson.call(repoJsonPath)
        if (forkRepoPath!=null){
            return forkRepoPath
        }


        // 如果是账号被封查不到API数据
        def flaggedAccount = ["hj987654321","jiushiyaojinqu"]
        for (String accountName: flaggedAccount){
            def forkPath = Paths.get(Util.forkDirectoryPath,"${accountName}@${repo.split('/')[1]}").normalize().toString()
            if(new File(forkPath).exists()){
                return forkPath
            }
        }
        return null
    }

    static boolean initTravisTriggerRepo(String triggerRepoPath,String repoInfo, String originRepoName, boolean isMerged){
        // 1.清空TravisTrigger文件夹的内容（保留.git)
        if(!emptyGitDirectory(triggerRepoPath)){
            println("清空触发仓库失败")
            return false
        }
        println("1.清空触发仓库成功")

        // 2. 目标仓库的绝对路径
        // merged:找到repo在forkDirectoryPath路径下对应的文件夹：PRCreator@repo
        // not merged:找到repo在Util.codeUnzipPath路径下对应的文件夹：owner-repo-commitNumber
        String originRepoPath = isMerged?initTriggerIsMerged(repoInfo):initTriggerNotMerged(originRepoName)
        if(originRepoPath==null){
            println("寻找目标仓库的绝对路径失败")
            return false
        }

        // 3.判断是否存在.travis.yml
        String travisPath = Paths.get(originRepoPath,'.travis.yml')
        if (!new File(travisPath).exists()){
            println(".travis.yml不存在")
            return false
        }
//        modifyTriggerBranch(travisPath)  // 修改travis的触发branch

        // 4.将目标仓库里的内容(除了.git)全部复制到triggerRepoPath中
        if(!copyAllFilesToAnotherDirectory(triggerRepoPath, originRepoPath)){
            println("copy失败")
            return false
        }
        println("copy成功")

        println("isMerged:${isMerged} --- ${originRepoName} 初始化完毕")
        return true
    }

    static String initTriggerIsMerged(String repoURL){
        def (repo, pulls) = urlToRepo(repoURL)
        try{
            //得到该repoURL在fork目录下对应的仓库的绝对路径
            String forkRepoPath = getForkRepoPath(repo, pulls)
            if (forkRepoPath==null){
                println("没有找到fork path")
                return null
            }
            println("fork path: ${forkRepoPath}")
            //在forkRepo切换到默认分支
            GitUtil.checkoutToDefaultBranch(forkRepoPath, repo)
            println("${repo} fork repo分支切换成功")
            return forkRepoPath
        }
        catch (Exception e){
            e.printStackTrace()
            println("${repo} fork repo分支切换失败")
            return null
        }
    }

    static String initTriggerNotMerged(String repoName){

        def originRepoPath = unzipCode(repoName)
        sleep(1000*30)
        return originRepoPath
        // 找到repo在Util.codeUnzipPath路径下对应的文件夹：owner-repo-commitNumber
//        def repoFile = new File(Util.codeUnzipPath).listFiles().find(){file->
//            if(file.name.contains(repoName.replace('/','-'))){
//                return true
//            }
//        }
//        if(repoFile == null){
//            println("没有找到对应的zip和解压文件")
//            return null
//        }
    }

    /**
     * 触发Travis Ci构建前的准备工作：
     * 1. 下载code.zip至 codeZipPath
     * 2. 解压code.zip至 codeUnzipPath
     */
    static void preparation(){
        //1.下载code.zip
        def repos = getAllRepos()
        println("repos的长度：" + repos.size())
        Pattern pattern = ~/(.*).zip/
        new File(Util.codeZipPath).listFiles().each {codeFile->
            Matcher matcher = pattern.matcher(codeFile.getName())
            if(matcher.find()){
                def downloadedRepo = matcher.group(1).replace('@','/')
                if(repos.contains(downloadedRepo)){
                    repos.remove(downloadedRepo)
                }
            }
        }
        println("需要下载的repos长度：" + repos.size())
        if(repos.size()!=0){
            def downloadError = downloads(repos)
            println("下载完毕")
            if (downloadError.size()>0){
                downloadError.each {
                    println("download error : ${it}")
                }
            }
        }

        //2. 解压code.zip
        repos.each {repoName->
            unzipCode(repoName)
        }
        println("准备工作结束")
    }

    static void getTravisCacheBuildTool(){
        ExcelUtil excel = new ExcelUtil(Util.PRDetailPath, 'TRAVIS_CACHE')
        def mergedRepo = excel.getCellsConditionally([2:"TRUE"],0)
        println("被merged的PR长度：" + mergedRepo.size() )
        mergedRepo = urlToRepo(mergedRepo)  //
        int travisCacheWithGradle = 0
        int travisCacheWithMaven = 0
        mergedRepo.each { repo ->
            def repository = MysqlUtil.getRepositoryByName(repo)
            if (repository == null) {
                println(repo)
            } else {
                if (repository.buildTool == 1) {
                    travisCacheWithMaven++
                } else {
                    travisCacheWithGradle++
                    println(repo)
                }
            }
        }
        println("Gradle的数量： ${travisCacheWithGradle}")
        println("Maven的数量： ${travisCacheWithMaven}")
    }

    static void main(String[] args) {
        ExcelUtil excel = new ExcelUtil(Util.PRDetailFilteredPath)
        excel.getSheets().each {sheet->
            excel.setSheet(sheet)
            println(sheet.sheetName)
            def noTravisRepo = excel.getCellsConditionally([0:"是",10:"no travis"],1)
            def noDataRepo = excel.getCellsConditionally([0:"是",10:""],1)
            def improvement = excel.getCellsConditionally([0:"是",10:""],1)
        }

        def mergedRepo = excel.getCellsConditionally([0:"是",10:"no travis"],1)

//        notMergedRepo = urlToRepo(notMergedRepo)  // owner/repo
//        mergedRepo = urlToRepo(mergedRepo)      // https://github.com/{owner/repo}/pull/{number}
    }
}
