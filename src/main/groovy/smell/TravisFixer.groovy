package smell

import org.yaml.snakeyaml.Yaml
import parser.YmlParser
import smell.fixer.Gradle.GradlePropertiesFixer
import util.GradleUtil.GradleStrategy
import util.TravisUtil.TravisStrategy

import java.util.regex.Matcher
import java.util.regex.Pattern

class TravisFixer {

    String TravisFilePath
    String TravisContent
    LinkedHashMap<String, Object> map
    List<String> keys = []

    TravisFixer(String travisFilePath) {
        this.TravisFilePath = travisFilePath
        this.TravisContent = new File(this.travisFilePath).text
        this.map = YmlParser.parse(this.TravisFilePath)
        YmlParser.getKeys(this.map, this.keys)
    }

    void travisSmellFixer(Map<TravisStrategy,StateFlag> strategyWithFlag, int buildTool){
        strategyWithFlag.each {strategy, flag->
            travisSmellFixer(strategy,buildTool,flag)
        }
    }

    void travisSmellFixer(TravisStrategy strategy, int buildTool, StateFlag flag){
        if (strategy == TravisStrategy.TRAVIS_CACHE) {
            if(flag==StateFlag.DEFAULT){
                //隐式引入
                addCache(buildTool)
            }else{
                //显式引入
                modifyCache(buildTool)
            }
        } else if (strategy == TravisStrategy.TRAVIS_SHALLOW_CLONE) {
            // modifyGitDepth()可以处理隐式引入和显式引入
            new File(this.TravisFilePath).text = modifyGitDepth()
        } else if (strategy == TravisStrategy.TRAVIS_RETRY && flag==StateFlag.CLOSE) {
            // 只有显式引入
            new File(this.TravisFilePath).text = removeTravisRetry()
        } else if (strategy == TravisStrategy.TRAVIS_WAIT && flag==StateFlag.CLOSE) {
            //只有显式引入
            new File(this.TravisFilePath).text = removeTravisWait()
        } else if (strategy == TravisStrategy.TRAVIS_FAST_FINISH) {
            if(flag==StateFlag.DEFAULT){
                //隐式引入
                new File(this.TravisFilePath).text = addFastFinish()
            }else{
                //显式引入
                modifyFastFinish()
            }
        }
    }

    // 显式引入和隐式引入:TRAVIS_SHALLOW_CLONE
    String modifyGitDepth() {
        if (keys.contains("git") && keys.contains("depth")) {
            String value = map.get("git").get("depth").toString()
            Pattern pattern = ~/(?ms)(git:.*?depth:.*?)${value}/
            Matcher matcher = pattern.matcher(TravisContent)
            return matcher.replaceFirst('$13')
        } else if (keys.contains("git")){
            Pattern pattern = ~/(?ms)git:.*?\n/
            Matcher matcher = pattern.matcher(TravisContent)
            return matcher.replaceFirst('$0  depth: 3\n')
        } else {
            return TravisContent + '\ngit:\n  depth: 3\n'
        }
    }

    // 只有显式引入的smell:TRAVIS_RETRY
    String removeTravisRetry() {
        if (TravisContent.contains("travis_retry")) {
            List<String> lines = TravisContent.split('\n')
            TravisContent = lines.collect {line ->
                if (line.contains("travis_retry") && line.trim().startsWith("#") == false) {
                    line = line.replaceAll(/\s*travis_retry\s+/, ' ')
                }
                line
            }.join('\n')
            return TravisContent + '\n'
        } else {
            throw new Exception("${this.TravisFilePath} 不包含Travis_retry")
        }
    }

    //  只有显式引入的smell:TRAVIS_WAIT
    String removeTravisWait() {
        if (TravisContent.contains("travis_wait")) {
            List<String> lines = TravisContent.split('\n')
            Pattern pattern = ~/travis_wait\s+(\d+)?/
            TravisContent = lines.collect {line ->
                if (line.contains("travis_wait") && !line.trim().startsWith("#")) {
                    Matcher matcher = pattern.matcher(line)
                    while (matcher.find()) {
                        println(matcher.groupCount())
                        if (matcher.group(1) == null) {
                            line = line.replaceAll(/\s*travis_wait\s+/, ' ')
                            break
                        } else {
                            int time = matcher.group(1).toInteger()
                            if (time > 10) {
                                line = line.replaceAll(/\s*travis_wait\s+(\d+)?\s+/, ' ')
                                break
                            }
                        }
                    }
                }
                line
            }.join('\n')
            return TravisContent + '\n'
        } else {
            throw new Exception("$TravisFilePath 不包含Travis_wait")
        }
    }

    // 隐式引入TRAVIS_CACHE
    void addCache(int buildTool = 1) {
        def outFile = new File(this.TravisFilePath)

        if (buildTool == 1) {
            outFile.withWriterAppend {writer ->
                writer.writeLine("\ncache:")
                writer.writeLine("  directories:")
                writer.writeLine('  - $HOME/.m2')
            }
        } else {
            outFile.withWriterAppend {writer ->
                writer.writeLine("\ncache:")
                writer.writeLine("  directories:")
                writer.writeLine('  - $HOME/.gradle/caches/')
                writer.writeLine('  - $HOME/.gradle/wrapper/')
            }
        }
    }

    // 显式引入TRAVIS_CACHE
    void modifyCache(int buildTool=1){
        def content = new File(this.TravisFilePath).readLines()
        content.eachWithIndex{  line,index ->
            if(line.trim().contains('cache:false')){
                if(buildTool==1){
                    content[index]='\ncache:\n  directories:\n  - $HOME/.m2\n'
                }else{
                    content[index]='\ncache:\n  directories:\n  - $HOME/.gradle/caches/\n  - $HOME/.gradle/wrapper/\n'
                }
            }
        }
        new File(this.TravisFilePath).text = content.join('\n')
    }

    // 隐式引入TRAVIS_FAST_FINISH
    String addFastFinish() {
        if (this.keys.contains("jobs")) {
            Pattern pattern = ~/(?m)(jobs:.*?)(\r\n|\r|\n)/
            Matcher matcher = pattern.matcher(TravisContent)
            return matcher.replaceFirst('$0  fast_finish: true\n')
        } else if (this.keys.contains("matrix")) {
            Pattern pattern = ~/(?m)(matrix:.*?)(\r\n|\r|\n)/
            Matcher matcher = pattern.matcher(TravisContent)
            return matcher.replaceFirst('$0  fast_finish: true\n')
        } else {
            return TravisContent + '\njobs:\n  fast_finish: true'
        }
    }

    // 显式引入TRAVIS_FAST_FINISH(项目数量为1，并且已经弃用travis ci)
    void modifyFastFinish() {
        def content = new File(this.TravisFilePath).readLines()
        content.eachWithIndex{  line,index ->
            if(line.trim().contains('fast_finish:false')){
                content[index]= line.replace('false','true')
            }
        }
        new File(this.TravisFilePath).text = content.join('\n')
    }

    /**
     * 修改.travis.yml文件，修改内容如下：
     * branches:
     *  only:
     *  - newBranchName
     * @param newBranchName
     */
    void updateBranches(String newBranchName){
        def only = ["only":[newBranchName]]
        this.map.put("branches",only)
        Yaml yaml = new Yaml()
        new File(this.TravisFilePath).text = yaml.dump(this.map)
    }

    /**
     * 将所有oraclejdk替换成openjdk
     */
    void updateJDK(){

    }
}
