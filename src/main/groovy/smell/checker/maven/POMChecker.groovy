package smell.checker.maven

import groovy.io.FileType
import maven.POM
import maven.POMTree
import model.Repository
import org.codehaus.groovy.ast.expr.BinaryExpression
import smell.StateFlag
import util.Util

import java.nio.file.Paths

import static util.MavenUtil.MavenStrategy

abstract class POMChecker {
//    POMTree pomTree
//    POM rootPom
//
//    POMChecker(String repoPath) {
//        this.pomTree = new POMTree(repoPath)
//        this.rootPom = pomTree.createPomTree()
//    }

    List<String> pomPaths = new ArrayList<>()
    List<String> smellPomPaths = new ArrayList<>()
    POMChecker(String repoPath) {
        File dir = new File(repoPath)
        dir.eachFileRecurse(FileType.FILES){
            if(it.name=='pom.xml'){
                this.pomPaths << it.absolutePath
            }
        }
        if(this.pomPaths.size()==0){
            println(repoPath + " 项目不含pom.xml文件")
        }
    }

    POMChecker(Repository repository) {
        this(Paths.get(Util.forkDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString())
    }

    abstract StateFlag check(MavenStrategy strategy)

    List<String> getSmellPomPaths(){
        return this.smellPomPaths
    }

    Map<MavenStrategy, StateFlag> check(List<MavenStrategy> strategies) {
        Map<MavenStrategy, StateFlag> map = new HashMap<>()
        for (MavenStrategy strategy : strategies) {
            StateFlag flag = check(strategy)
            map.put(strategy, flag)
        }
        return map
    }

    static Map<MavenStrategy, Closure> predicatesMap = [
            (MavenStrategy.MAVEN_PARALLEL_TEST): { Map<String, String> testConfigurations ->
                String parallel = testConfigurations.get("parallel")
                String unlimited = testConfigurations.get("useUnlimitedThreads")
                String perCoreCount = testConfigurations.get("perCoreThreadCount")
                String threadCount = testConfigurations.get("threadCount")
                if(parallel == null) {
                    return StateFlag.DEFAULT
                }
                println("parallel:" + parallel)
                println("threadCount:"+threadCount)
                println("perCoreCount:"+perCoreCount)
                println("unlimited:"+unlimited)
                if(parallel == "none" || parallel == "false") {
                    return StateFlag.CLOSE
                } else if(threadCount==1 && perCoreCount =='false' && (unlimited ==null||unlimited=='false')) {
                    return StateFlag.CLOSE
                }
                return StateFlag.OPEN
            },

            (MavenStrategy.MAVEN_FORK_TEST): { Map<String, String> testConfigurations ->
                String val = testConfigurations.get("forkCount")
                if(val == null) {
                    return StateFlag.DEFAULT
                }
                println("forkCount:"+val)
                if(val == "1" || val == "0"){
                    return StateFlag.CLOSE
                } else{
                    return StateFlag.OPEN
                }
            },

            (MavenStrategy.MAVEN_REPORT_GENERATION): { Map<String, String> testConfigurations ->
                String val = testConfigurations.get("disableXmlReport")
                if(val == null) {
                    return StateFlag.DEFAULT
                }
                println("disableXmlReport:"+val)
                if (val == "true"){
                    return StateFlag.OPEN
                } else {
                    return StateFlag.CLOSE
                }
            },

            (MavenStrategy.MAVEN_COMPILER_DAEMON): { Map<String, String> compilationConfigurations ->
                String val = compilationConfigurations.get("fork")
                if(val == null) {
                    return StateFlag.DEFAULT
                }
                println("fork:"+val)
                if (val == "true"){
                    return StateFlag.OPEN
                } else {
                    return StateFlag.CLOSE
                }
            },

//            (MavenStrategy.MAVEN_INCREMENTAL_COMPILATION): { Map<String, String> compilationConfigurations ->
//                String val = compilationConfigurations.get("useIncrementalCompilation")
//                //println("'"+val+"'")
//                if(val == null) {
//                    return StateFlag.DEFAULT
//                } else if (val.equals("true")){
//                    return StateFlag.OPEN
//                } else {
//                    return StateFlag.CLOSE
//                }
//            },
    ]
}
