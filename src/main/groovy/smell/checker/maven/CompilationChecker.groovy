package smell.checker.maven

import maven.POM
import maven.POMParser
import model.Repository
import smell.StateFlag
import util.MavenUtil.MavenStrategy

class CompilationChecker extends POMChecker {
    CompilationChecker(String repoPath) {
        super(repoPath)
    }

    CompilationChecker(Repository repository) {
        super(repository)
    }

    @Override
    StateFlag check(MavenStrategy strategy) {
        //这里只检测的根pom，后面根据实际情况看是否要检测其它pom文件
//        Map<String, String> compilationConfigurations = this.rootPom.getCompilationConfigurations();
//        Closure predicate = POMChecker.predicatesMap.get(strategy)
//        return predicate.call(compilationConfigurations)

        Closure predicate = POMChecker.predicatesMap.get(strategy)
        def open = []
        def close = []
        this.pomPaths.each {pomPath->
            POM pom = new POMParser().parse(pomPath)
            Map<String, String> testConfigurations = pom.getTestConfigurations()
            if (testConfigurations.keySet().size()==0){
                return
            }
            def flag = predicate.call(testConfigurations)
            if (flag == StateFlag.CLOSE){
                close << pomPath
            }else if (flag == StateFlag.OPEN){
                open << pomPath
            }
        }

        if(open.size()!=0 && close.size()!=0){
            print("同时存在CLOSE: ${close.size()}  和OPEN: ${open.size()}" )
        }

        if(open.size()==0 && close.size()==0){
            return StateFlag.DEFAULT
        }else if (open.size()!=0){
            return StateFlag.OPEN
        }
        return StateFlag.CLOSE
    }
}
