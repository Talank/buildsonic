package smell.checker

import groovy.transform.PackageScope
import parser.YmlParser
import smell.StateFlag
import smell.checker.maven.ParallelBuildChecker
import util.GradleUtil

import static util.TravisUtil.*


import java.util.regex.Matcher

class TravisChecker {
    //@PackageScope String TravisFilePath = null
    String TravisFilePath
    String TravisContent
    LinkedHashMap<String, Object> map
    List<String> commands
    List<String> keys = []
    TravisChecker(String travisFilePath) {
        this.TravisFilePath = travisFilePath
        this.TravisContent = new File(this.travisFilePath).text
        this.map = YmlParser.parse(this.TravisFilePath)
        this.commands = YmlParser.getCommands(map)
        YmlParser.getKeys(this.map, this.keys)
    }

    // issue:如果方法头部写成 Integer shallowCloneValue()
    // 在shallowCloneCheck()中可以正常调用，但是在TravisTrigger中就不能正常调用这个方法
    // 将Integer改为def后运行成功
    def shallowCloneValue(){
        if (keys.contains("git") && keys.contains("depth")) {
            String value = map.get("git").get("depth").toString()
            if(value.contains('false')){
                return -1
            }else if(value.isInteger()){
                 return value.toInteger()
            }
        }
        return null
    }

    StateFlag shallowCloneCheck() {
        def depth = shallowCloneValue()
        if(depth == null){
            return StateFlag.DEFAULT
        }else if(depth==-1 || depth >=50){
            return StateFlag.CLOSE
        }else if(depth < 50){
            return StateFlag.OPEN
        }
    }

    StateFlag retryCheck() {
        List<String> result = []
        commands.each {
            if (it.contains("travis_retry")) {
                result << it
            } else {
                Matcher matcher = (it =~ /--retry (\d+)/)
                if (matcher && matcher.group(1).toInteger() > 1) {
                    result << it
//                    println(matcher.group(1).toInteger())
                }
            }
        }
        //字段travis_retry在数据库中为null表示消除smell（StateFlag.OPEN）
        return result.size()>=1?StateFlag.CLOSE:StateFlag.OPEN
    }

    StateFlag waitCheck() {
        List<Integer> result = []
        commands.each {
            if (it.contains("travis_wait")) {
                Matcher matcher = (it =~ /travis_wait (\d+)/)
                if (matcher) {
                    int waitTime = matcher.group(1).toInteger()
                    if(waitTime>10){
                        result << matcher.group(1).toInteger()
                    }
                } else {
                    result << 20
                }
            }
        }
        //字段travis_wait在数据库中为null表示消除smell（StateFlag.OPEN）
        return result.size()>0?StateFlag.CLOSE:StateFlag.OPEN
    }

    StateFlag cacheCheck() {
        def cache = map?.get("cache")
        if (cache == null) {
            return StateFlag.DEFAULT
        }
        List<Object> result = []
        YmlParser.getCacheValues(map, result)
        if (result.size() == 0) {
            return StateFlag.DEFAULT
        }
        return result.any { it != false }?StateFlag.OPEN:StateFlag.CLOSE
    }

    StateFlag fastFinishCheck() {
        List<Object> allowFailuresList = []
        YmlParser.getKeyValues(map, allowFailuresList, "allow_failures")
        Boolean allow_failures = allowFailuresList.any { it != null }
        if(!allow_failures){
            return null  // 如果没有设置allow_failures=true,属于隐式消除，先用null替代
        }
        List<Object> fastFinishList = []
        YmlParser.getKeyValues(map, fastFinishList, "fast_finish")
        if (fastFinishList.size() > 0) {
            return fastFinishList.any { it == true }?StateFlag.OPEN:StateFlag.CLOSE
        }
        return StateFlag.DEFAULT  // 虽然这个smell没有隐式引入了，但是没有配置fast_finish和fast_finish=false要分开处理
    }

    StateFlag check(TravisStrategy strategy){
        if (strategy == TravisStrategy.TRAVIS_SHALLOW_CLONE) {
            return shallowCloneCheck()
        } else if (strategy == TravisStrategy.TRAVIS_RETRY) {
            return retryCheck()
        } else if (strategy == TravisStrategy.TRAVIS_WAIT) {
            return waitCheck()
        } else if (strategy == TravisStrategy.TRAVIS_CACHE) {
            return cacheCheck()
        } else if (strategy == TravisStrategy.TRAVIS_FAST_FINISH) {
            return fastFinishCheck()
        }
    }
    
//     void run() {
//        try (Session session = SessionUtil.getSession()) {
//            Transaction tx =session.beginTransaction();
//            List<Repository> repositories = MysqlUtil.getRepositories(session)
//            for (Repository repository : repositories) {
//                //shallow clone
//                /*
//                def shallowCloneValue = shallowCloneCheck(repository)
//                if (shallowCloneValue != null) {
//                    repository.setTravisGitDepth(shallowCloneValue.toString())
//                }
//
//                //travis_retry
//                List<String> travisRetryResult = retryCheck(repository)
//                if (travisRetryResult.size() > 0) {
//                    repository.setTravisRetry(true)
//                } else {
//                    repository.setTravisRetry(false)
//                }
//
//                //travis_wait
//                List<Integer> travisWaitResult = waitCheck(repository)
//                if (travisWaitResult.size() > 0) {
//                    repository.setTravisWait(travisWaitResult)
//                }
//
//
//                //cache
//                def result = cacheCheck(repository)
//                if (result == null) {
//                    repository.setTravisCache(null)
//                } else if (result == false) {
//                    repository.setTravisCache(false)
//                } else {
//                    repository.setTravisCache(true)
//                }
//
//                 */
//
//                //fast_finish
//                def (allow_failures, fast_finish) = fastFinishCheck(repository)
//                repository.setTravisAllowFailures(allow_failures)
//                repository.setTravisFastFinish(fast_finish)
//            }
//            tx.commit()
//        }
//    }
    
     static void main(String[] args) {
        //TravisSmell.shallowCloneCheck()
        //TravisSmell.retryCheck()
        //TravisSmell.waitCheck()
        //TravisSmell.cacheCheck()
        //TravisChecker.fastFinishCheck()
//        run()
    }

    void gradleCommandChecker(GradleUtil.GradleStrategy strategy){
        List<String> content = new File(this.TravisFilePath).readLines()
        if(strategy == GradleUtil.GradleStrategy.PARALLEL_BUILDS){
            content.eachWithIndex{line,index->
                if(line.contains('--no-parallel')){
                    println(line)
                }
                if(line=~ /org.gradle.parallel\s*=\s*false/){
                    println(line)
                }
            }
        }
        if(strategy == GradleUtil.GradleStrategy.FILE_SYSTEM_WATCHING){
            content.eachWithIndex{line,index->
                if(line.contains('--no-watch-fs')){
                    println(line)
                }
                if(line=~ /org.gradle.vfs.watch\s*=\s*false/){
                    println(line)
                }
            }
        }
        if(strategy == GradleUtil.GradleStrategy.CONFIGURATION_ON_DEMAND){
            content.eachWithIndex{line,index->
                if(line.contains('--no-configure-on-demand')){
                    println(line)
                }
                if(line=~ /org.gradle.configureondemand\s*=\s*false/){
                    println(line)
                }
            }
        }
        if(strategy == GradleUtil.GradleStrategy.CACHING){
            content.eachWithIndex{line,index->
                if(line.contains('--no-build-cache')){
                    println(line)
                }
                if(line=~ /org.gradle.caching\s*=\s*false/){
                    println(line)
                }
            }
        }
        if (strategy == GradleUtil.GradleStrategy.GRADLE_DAEMON){
            content.eachWithIndex{line,index->
                if(line.contains('--no-daemon')){
                    println(line)
                }
                if(line=~ /org.gradle.daemon\s*=\s*false/){
                    println(line)
                }
            }
        }
    }
}
