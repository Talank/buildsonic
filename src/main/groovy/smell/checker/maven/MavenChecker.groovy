package smell.checker.maven

import model.Repository
import smell.StateFlag
import smell.checker.maven.CompilationChecker
import smell.checker.maven.POMChecker
import smell.checker.maven.TestChecker
import smell.fixer.Maven.ParallelBuildFixer

import static util.MavenUtil.*

class MavenChecker {

    //判断项目是否适合使用该策略：检查前置条件，用于自动发PR
    static StateFlag check(Repository repository,String repoPath,MavenStrategy strategy) {
        if (strategy == MavenStrategy.MAVEN_PARALLEL_EXECUTION) {
            return checkParallelBuild(repository)
        }else if(getMavenCategory(strategy)== MavenCategory.TEST || getMavenCategory(strategy)==MavenCategory.FORK){
            return checkMavenTest(repository, repoPath,strategy)
        } else {
            return checkMavenCompilation(repository,repoPath,strategy)
        }


    }

    //不判断前置条件:用于自动触发travis ci
    static StateFlag check(String repoPath,String originRepoName,MavenStrategy strategy) {
        if (strategy == MavenStrategy.MAVEN_PARALLEL_EXECUTION) {
            return ParallelBuildChecker.parallelExecutionChecker(repoPath,originRepoName)
        }else if(getMavenCategory(strategy)== MavenCategory.TEST || getMavenCategory(strategy)==MavenCategory.FORK){
            POMChecker checker = new TestChecker(repoPath)
            def flag = checker.check(strategy)
            if(flag!=StateFlag.DEFAULT && flag!=null){
                println(originRepoName + " : "+flag.toString()+"\n")
            }
            return flag
        } else {
            POMChecker checker = new CompilationChecker(repoPath)
            return checker.check(strategy)
        }
    }

    static StateFlag checkMavenTest(Repository repository, String repoPath, MavenStrategy strategy){
        //前置条件
//        if(repository.containTest==null || !repository.containTest){
//            println("${repository.repoName} 没有Test文件")
//            return null
//        }
//        if(strategy == MavenStrategy.MAVEN_PARALLEL_TEST && !repository.getMultiModule()){
//            return null
//        }
        POMChecker checker = new TestChecker(repoPath)
        def flag = checker.check(strategy)
//        if(flag!=StateFlag.DEFAULT && flag!=null){
//            println(repository.repoName + " : "+flag.toString()+"\n")
//        }
        return flag
    }

    static StateFlag checkMavenCompilation(Repository repository,String repoPath, MavenStrategy strategy){
        //前置条件
//        if(strategy == MavenStrategy.MAVEN_COMPILER_DAEMON && repository.getJavaFilesNum()<1000){
//            return null
//        }
        POMChecker checker = new CompilationChecker(repoPath)
        return checker.check(strategy)
    }

    static StateFlag checkParallelBuild(Repository repository){
        //前置条件
        if(!repository.multiModule) {
            return null
        }
        return ParallelBuildChecker.parallelExecutionChecker(repository)
    }
}
