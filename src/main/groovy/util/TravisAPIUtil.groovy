package util


import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern


//token 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
class TravisAPIUtil {
    /**
     * 考虑到并行化测试，假设有10个trigger仓库,则需要对每个trigger仓库进行info的爬取
     * 1.首先爬取的是这个trigger repo对应的builds信息：所有在travis上触发的build
     * curl -H "Travis-API-Version: 3" -H "User-Agent: API Explorer"   -H "Authorization: token 0KJJ5DIhcjI6hju2l3qfcA"  https://api.travis-ci.com/repo/i-Taozi%2F${strategyName}Trigger/builds?limit=100 > ${buildsInfoPath}
     * curl -H "Travis-API-Version: 3" -H "User-Agent: API Explorer"   -H "Authorization: token 0KJJ5DIhcjI6hju2l3qfcA"  "https://api.travis-ci.com/repo/i-Taozi%2FGradlePropertyTrigger/builds?limit=100" > ./gradlePropertyInfo1.json
     */


    static def getAndSaveBuildsInfo(String strategyName, boolean offset=false){
        //  GET  /repo/{repository.slug}/builds
        Closure GETBuilds = { int retry=3 ->
            int times = 0
            while(times++<retry){
                String repositorySlug = "i-Taozi%2F${strategyName}_Trigger"
                String travisToken = "token 0KJJ5DIhcjI6hju2l3qfcA"
                String host = "https://api.travis-ci.com/repo/${repositorySlug}/builds?limit=100"
                if(offset){
                    host = "https://api.travis-ci.com/repo/${repositorySlug}/builds?limit=100&offset=100"
                }
                String buildsInfoPath = Util.getTravisAPIInfoPath(strategyName)
                if(offset){
                    buildsInfoPath = Util.getTravisAPIInfoPath(strategyName+"_1")
                }
                def getBuildsInfoCommand = "curl -H \"Travis-API-Version: 3\" -H \"User-Agent: API Explorer\"   -H \"Authorization: ${travisToken}\"  ${host} > ${buildsInfoPath}"
                println(getBuildsInfoCommand)
                def process = [ 'bash', '-c', getBuildsInfoCommand ].execute()
                process.waitFor()
                println process.err.text
                println process.text
//                if(process.err.text.size()!=0){
//                    println("curl语句执行错误： " + process.err.text)
//                    println process.text
//                    continue
//                }
                def buildsInfo = new JsonSlurper().parse(new File(buildsInfoPath))
                if(buildsInfo['@type'] == 'error'){
                    println("爬取信息失败：${strategyName}")
                    println("error_type : " + buildsInfo['error_type'])
                    println("error_message : " + buildsInfo['error_message'])
                }else{
                    return buildsInfo
                }
            }
            return null
        }
        return GETBuilds.call()
    }

    static def getBuildsInfo(String strategyName) {
        def buildsInfo = getAndSaveBuildsInfo(strategyName)
        if(buildsInfo==null){
            return null
        }
        Integer maxCount = buildsInfo['@pagination']['count'].toString().toInteger()
        if(maxCount!=null &&  maxCount> 100){
            buildsInfo = getAndSaveBuildsInfo(strategyName,true)
        }
        return buildsInfo
    }

    static List<String> getTriggeredRepo(String strategyName){
        String triggeredRepoInfoPath = Util.getTravisAPIInfoPath(strategyName)
        if(!new File(triggeredRepoInfoPath).exists()){
            return []
        }
        def buildsInfo = new JsonSlurper().parse(new File(triggeredRepoInfoPath))
        List<String> triggeredRepo = new ArrayList<>()
        buildsInfo['builds'].each { build->
            def branch = build['branch']['name']
            if(branch=='main'){
                def title = build['commit']['message']
                def repoName = title.toString().split(':')[0]
                triggeredRepo << repoName
            }
        }
        return triggeredRepo.unique()
    }

    /**
     * 写文件：'PR时间对比.xlsx'，加上 synchronized(TravisAPIUtil.class)
     * 保证在同一时刻至多只有一个线程可以调用TravisAPIUtil的这个静态方法
     * @param strategyName
     * @param buildsInfo：通过travis api获取的json数据
     * @return
     */
    static void parserBuildsInfo(String strategyName, def buildsInfo){
        String excelPath = Paths.get(System.getProperty("user.dir"), "resources", "PR时间对比.xlsx")
        synchronized(TravisAPIUtil.class){
            ExcelUtil excel = new ExcelUtil(excelPath,strategyName)
            buildsInfo['builds'].each { build->
                def branch = build['branch']['name']
                if(branch != 'main'){
                    return
                }
                def state = build['state']
                def duration =  state=='passed'?build['duration']:state
                def title = build['commit']['message']
                String repoName = title.toString().split(':')[0]
                if(title.toString().contains("original code")){
                    excel.addContentByRepoName(strategyName,repoName,["originTime":duration.toString()])
                }else{
                    excel.addContentByRepoName(strategyName,repoName,["fixedTime":duration.toString()])
                }
            }
        }
    }

    static boolean restartBuild(){
        def cacheStrategy = ["CACHING","TRAVIS_CACHE"]
        cacheStrategy.each {cache->
            restartBuild(cache)
        }
    }

    static boolean restartBuild(String cacheStrategy){
        def cacheBuildInfo = getBuildsInfo(cacheStrategy)
        if(cacheBuildInfo==null){
            return false
        }
        cacheBuildInfo['builds'].each {
            def state = cacheBuildInfo['state']
            def buildId =  cacheBuildInfo['id']
            def title = cacheBuildInfo['commit']['message']
            if(state=='passed' && !title.toString().contains('origin code')){
                String travisToken = 'token 0KJJ5DIhcjI6hju2l3qfcA'
                String host = "https://api.travis-ci.com/build/${buildId}/restart"
                def restartCommand = "curl -X POST -H \"Content-Type: text/yaml\" -H \"Travis-API-Version: 3\" -H \"User-Agent: API Explorer\" -H \"Authorization: ${travisToken}\"  ${host}"
                def process = [ 'bash', '-c', restartCommand ].execute()
                process.waitFor()
                if(process.err.text.size()!=0){
                    println("curl语句执行错误： " + process.err.text)
                    println process.text
                }
            }
        }
    }
}



