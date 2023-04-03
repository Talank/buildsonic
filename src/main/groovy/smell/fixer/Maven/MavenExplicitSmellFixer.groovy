package smell.fixer.Maven

import maven.POM
import maven.POMParser
import maven.POMTree
import org.apache.poi.ss.formula.functions.T
import smell.StateFlag
import smell.checker.maven.POMChecker
import smell.checker.maven.TestChecker
import util.MavenUtil.MavenStrategy

import java.nio.file.Paths

class MavenExplicitSmellFixer {
    String repoPath
    MavenStrategy strategy

    MavenExplicitSmellFixer(String repoPath, MavenStrategy strategy) {
        this.repoPath = repoPath
        this.strategy = strategy
    }

    Boolean fixer(){
        POMChecker checker = new TestChecker(this.repoPath)
        def flag = checker.check(strategy)
        if(flag!=StateFlag.CLOSE){
            return flag
        }
        def smellPomPaths = checker.getSmellPomPaths()
        if(smellPomPaths.size()==0){
            println("检测出显式，但是目标pom不存在")
            return null
        }
        smellPomPaths.each {pomPath->
            try{
                def fixed = false
                if(this.strategy == MavenStrategy.MAVEN_PARALLEL_TEST){
                    fixed = parallelTestFixer(pomPath)
                }
                if(this.strategy == MavenStrategy.MAVEN_FORK_TEST){
                    fixed = forkTestFixer(pomPath)
                }
                if(this.strategy == MavenStrategy.MAVEN_REPORT_GENERATION){
                    fixed = reportGenerationFixer(pomPath)
                }
                if (!fixed){
                    println("显式引入修复失败: ${pomPath}")
                }
            }catch (Exception e){
                println(e.toString())
                println("显式引入修复error: ${pomPath}")
            }
        }
        return true
    }

    static Boolean forkTestFixer(String pomPath){
        POMFixer fixer = new POMFixer(pomPath)
        def flag=  fixer.updatePluginNodeConfiguration('maven-surefire-plugin','forkCount','1.5C')
        fixer.printToFile(pomPath)
        return flag
    }

    static Boolean reportGenerationFixer(String pomPath){
        POMFixer fixer = new POMFixer(pomPath)
        def flag = fixer.updatePluginNodeConfiguration('maven-surefire-plugin','disableXmlReport','true')
        fixer.printToFile(pomPath)
        return flag
    }

    static Boolean parallelTestFixer(String pomPath){
        POMFixer fixer = new POMFixer(pomPath)

        POM pom = new POMParser().parse(pomPath)
        Map<String, String> testConfigurations = pom.getTestConfigurations()
        String parallel = testConfigurations.get("parallel")
        String unlimited = testConfigurations.get("useUnlimitedThreads")
        String perCoreCount = testConfigurations.get("perCoreThreadCount")
        String threadCount = testConfigurations.get("threadCount")

        if(parallel == null || testConfigurations.size()==0) {
            return false
        }

        if(parallel=="none" || parallel=="false") {
            fixer.updatePluginNodeConfiguration('maven-surefire-plugin','parallel','classes')
            if(unlimited==null){
                // parallel在pluginManagement里面，这个被插入到其它地方？可不可能出现？
                fixer.editSurefireNode(['useUnlimitedThreads':'true'])
            }else if(unlimited=='false'){
                fixer.updatePluginNodeConfiguration('maven-surefire-plugin','useUnlimitedThreads','true')
            }
            fixer.printToFile(pomPath)
        } else if(threadCount==1 && perCoreCount =='false' && (unlimited ==null||unlimited=='false')) {
            if(unlimited==null){
                // parallel在pluginManagement里面，这个被插入到其它地方？可不可能出现？
                fixer.editSurefireNode(['useUnlimitedThreads':'true'])
            }else if(unlimited=='false'){
                fixer.updatePluginNodeConfiguration('maven-surefire-plugin','useUnlimitedThreads','true')
            }
            fixer.printToFile(pomPath)
        }
        return true
    }
}
