package smell.fixer.Gradle


import parser.OptionParser
import smell.StateFlag
import util.Util
import smell.checker.gradle.BuildGradleChecker
import java.nio.file.Paths

import static util.GradleUtil.*

class GradleExplicitSmellFixer {
    String repoName
    String repoPath
    List<String> buildFilePaths
    String travisFilePath
    GradleStrategy strategy
    GradleCategory category

    GradleExplicitSmellFixer(String originRepoName,String repoPath) {
        this.repoName = originRepoName
        this.repoPath = repoPath
        this.buildFilePaths = Util.getGradleFilePaths(repoPath)
        this.travisFilePath = Paths.get(repoPath,".travis.yml").normalize().toString()
    }

    void fixer(GradleStrategy strategy){
        this.strategy = strategy
        this.category = getGradleCategory(this.strategy)
        println("修复显式引入Gradle smell---${this.strategy.toString()} caregory:${this.category.toString()}")
        println("smell类型属于：${category == GradleCategory.PROPERTIES?'PROPERTIES':'BUILD'}")
        if (category == GradleCategory.TEST || category == GradleCategory.COMPILATION || category == GradleCategory.FORK){
            buildFixer()
        } else if (category == GradleCategory.PROPERTIES){
            propertiesFixer()
        }
    }

    void buildFixer(){
        buildFilePaths.each {buildFilePath->
            def flag = BuildGradleChecker.singleFileCheck(buildFilePath, strategy)
            if(flag == StateFlag.CLOSE){
                println("显式引入的文件:${buildFilePath}" )
                List<String> buildFileContent = new File(buildFilePath).readLines()
                if (buildFixer(buildFileContent)){
                    new File(buildFilePath).text = buildFileContent.join('\n')
                }
            }
        }
    }

    boolean buildFixer(List<String> buildFileContent){
        boolean changed = false
        if(this.strategy == GradleStrategy.GRADLE_COMPILER_DAEMON) {
            buildFileContent.eachWithIndex{ line, index ->
                if(line.replace(" ","").contains('options.fork')){
                    println(index+":"+line)
                    buildFileContent[index] = line.replace('false','true')
                    changed = true
                }
            }
        }
        if(this.strategy == GradleStrategy.GRADLE_INCREMENTAL_COMPILATION) {
            buildFileContent.eachWithIndex{ line, index ->
                if(line.replace(" ","").contains('options.incremental')){
                    println(index+":"+line)
                    buildFileContent[index] = line.replace('false','true')
                    changed = true
                }
            }
        }
        if(this.strategy == GradleStrategy.GRADLE_PARALLEL_TEST){
            buildFileContent.eachWithIndex{ line, index ->
                if(line.replace(" ","").contains('maxParallelForks')){
                    println(index+":"+line)
                    buildFileContent[index] = line.replace('1','Runtime.runtime.availableProcessors().intdiv(2) ?: 1')
                    changed = true
                }
            }
        }
        if(this.strategy == GradleStrategy.GRADLE_FORK_TEST) {
            buildFileContent.eachWithIndex{ line, index ->
                if(line.replace(" ","").contains('forkEvery')){
                    println(index+":"+line)
                    buildFileContent[index] = line.replace('1','100')
                    changed = true
                }
            }
        }
        if (this.strategy ==GradleStrategy.GRADLE_REPORT_GENERATION){
            buildFileContent.eachWithIndex{ line, index ->
                if(line.replace(" ","").contains('html.required') || line.trim().contains('junitXml.required')){
                    println(index+":"+line)
                    buildFileContent[index] = line.replace('true','false')
                    changed = true
                }
            }
        }
        return changed
    }

    void propertiesFixer(){
        OptionParser optionParser = new OptionParser(this.repoPath,this.repoName)
        def gradleProperties = optionParser.parseGradleProperties()
        def shellBuildCommandsMap = optionParser.getShellBuildCommandsMap()
        gradlePropertiesFixer(gradleProperties)
        commandsFixer(shellBuildCommandsMap)
    }

    void commandsFixer(Map<String, List<String>> shellBuildCommandsMap){
        Closure commandFixer = { String commandFilePath->
            List<String> content = new File(commandFilePath).readLines()
            boolean changed = false
            if(strategy == GradleStrategy.PARALLEL_BUILDS){
                content.eachWithIndex{line,index->
                    if(line.contains('--no-parallel')){
                        content[index] = line.replace('--no-parallel','--parallel')
                        changed=true
                    }
                    if(line=~ /org.gradle.parallel\s*=\s*false/){
                        content[index] = line.replace('false','true')
                        changed=true
                    }
                }
            }
            if(strategy == GradleStrategy.FILE_SYSTEM_WATCHING){
                content.eachWithIndex{line,index->
                    if(line.contains('--no-watch-fs')){
                        content[index] = line.replace('--no-watch-fs','--watch-fs')
                        changed=true
                    }
                    if(line=~ /org.gradle.vfs.watch\s*=\s*false/){
                        content[index] = line.replace('false','true')
                        changed=true
                    }
                }
            }
            if(strategy == GradleStrategy.CONFIGURATION_ON_DEMAND){
                content.eachWithIndex{line,index->
                    if(line.contains('--no-configure-on-demand')){
                        content[index] = line.replace('--no-configure-on-demand','--configure-on-demand')
                        changed=true
                    }
                    if(line=~ /org.gradle.configureondemand\s*=\s*false/){
                        content[index] = line.replace('false','true')
                        changed=true
                    }
                }
            }
            if(strategy == GradleStrategy.CACHING){
                content.eachWithIndex{line,index->
                    if(line.contains('--no-build-cache')){
                        content[index] = line.replace('--no-build-cache','--build-cache')
                        changed=true
                    }
                    if(line=~ /org.gradle.caching\s*=\s*false/){
                        content[index] = line.replace('false','true')
                        changed=true
                    }
                }
            }
            if(strategy == GradleStrategy.GRADLE_DAEMON){
                content.eachWithIndex{line,index->
                    if(line.contains('--no-daemon')){
                        content[index] = line.replace('--no-daemon','--daemon')
                        changed=true
                    }
                    if(line=~ /org.gradle.daemon\s*=\s*false/){
                        content[index] = line.replace('false','true')
                        changed=true
                    }
                }
            }
            if(changed){
                new File(commandFilePath).text = content.join('\n')
            }
        }
        // 修改.travis.yml中的commands
        if(new File(this.travisFilePath).exists()){
            commandFixer.call(this.travisFilePath)
        }
        // 修改shell中的commands
        shellBuildCommandsMap.each {shellFile,shellCommands->
            String shellFilePath = Paths.get(this.repoPath,shellFile).normalize().toString()
            commandFixer.call(shellFilePath)
        }
    }

    void gradlePropertiesFixer(Map<String, String> gradleProperties){
        def strategies = [strategy]   //调用GradlePropertiesFixer(传入List<>)
        if(this.strategy == GradleStrategy.PARALLEL_BUILDS){
            if(gradleProperties.get("org.gradle.parallel") == "false"){
                GradlePropertiesFixer.modifyProperties(this.repoPath,strategies)
            }
        }
        if(this.strategy == GradleStrategy.FILE_SYSTEM_WATCHING){
            if(gradleProperties.get("org.gradle.vfs.watch") == "false"){
                GradlePropertiesFixer.modifyProperties(this.repoPath,strategies)
            }
        }
        if(this.strategy == GradleStrategy.CONFIGURATION_ON_DEMAND){
            if(gradleProperties.get("org.gradle.configureondemand") == "false"){
                GradlePropertiesFixer.modifyProperties(this.repoPath,strategies)
            }
        }
        if(this.strategy == GradleStrategy.CACHING){
            if(gradleProperties.get("org.gradle.caching") == "false"){
                GradlePropertiesFixer.modifyProperties(this.repoPath,strategies)
            }
        }
        if(this.strategy == GradleStrategy.GRADLE_DAEMON){
            if(gradleProperties.get("org.gradle.daemon") == "false"){
                GradlePropertiesFixer.modifyProperties(this.repoPath,strategies)
            }
        }
    }

    static void main(String[] args) {

    }
}
