package smell.checker.gradle

import model.Repository
import parser.GradleDependency
import parser.GradleParser

import static util.GradleUtil.StateFlag
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

class GradleExtractor {
    static void gradleVersionChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        List<String> a =[]
        List<String> b =[]
        for (Repository repository : repositories) {
            if (repository.buildTool != 2)
                continue
            String version = gradleLintChecker(repository)
        }
    }

    static void dynamicVersions(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        List<String> gradleFilePaths = Util.getGradleFilePaths(repoPath)
        if (gradleFilePaths.size() == 0)
            return
        //println(gradleFilePaths)
        gradleFilePaths.each {filePath ->
            println(filePath)
            File file = new File(filePath)
            GradleParser parser = new GradleParser(file)
            List<GradleDependency> dependencies = parser.getAllDependencies()
            if (dependencies.size() > 0) {
                println(dependencies)
            }
        }
    }

    static StateFlag gradleLintChecker(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        List<String> gradleFilePaths = Util.getGradleFilePaths(repoPath)
        if (gradleFilePaths.size() == 0)
            return
        for (String filePath :gradleFilePaths) {
            println(repository.repoName)
            String content = new File(filePath).text
            if (content.contains("nebula.lint") == false)
                continue
            println(content)
            println("==========================================")
//            GradleParser parser = new GradleParser(filePath)
//            GradleVisitor visitor = new GradleVisitor()
//            parser.walkScript(visitor)
//            List<BinaryExpression> expressions = visitor.binaryExpressions
//            for (BinaryExpression expression : expressions) {
//                if (expression.leftExpression.text.endsWith("options.incremental")) {
//                    if (expression.rightExpression.text.equals("false")) {
//                        return STATE_FLAG.CLOSE
//                    } else {
//                        return STATE_FLAG.OPEN
//                    }
//                }
//            }
        }
        return GradleOptionChecker.STATE_FLAG.DEFAULT
    }

    static void gradleLintChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        Map<StateFlag, Integer> map = new HashMap<>()
        List<String> a =[]
        List<String> b =[]
        for (Repository repository : repositories) {
            if (repository.buildTool != 2)
                continue
            StateFlag flag = gradleLintChecker(repository)
            if (flag == GradleOptionChecker.STATE_FLAG.OPEN) {
                map[GradleOptionChecker.STATE_FLAG.OPEN] = map.getOrDefault(GradleOptionChecker.STATE_FLAG.OPEN, 0) + 1
                a << repository.repoName
            } else if (flag == GradleOptionChecker.STATE_FLAG.CLOSE) {
                map[GradleOptionChecker.STATE_FLAG.CLOSE] = map.getOrDefault(GradleOptionChecker.STATE_FLAG.CLOSE, 0) + 1
                b << repository.repoName
            } else if (flag == GradleOptionChecker.STATE_FLAG.DEFAULT)
                map[GradleOptionChecker.STATE_FLAG.DEFAULT] = map.getOrDefault(GradleOptionChecker.STATE_FLAG.DEFAULT, 0) + 1
        }
        println(a)
        println(b)
        println(map)
    }
}
