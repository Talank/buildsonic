package smell.checker.gradle

import model.Repository
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.hibernate.Session
import org.hibernate.Transaction
import parser.GradleParser
import parser.GradleVisitor
import smell.StateFlag
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

import static util.GradleUtil.GradleStrategy

class BuildGradleChecker {
    static Map<GradleStrategy, Closure> predicatesMap = [
            (GradleStrategy.GRADLE_COMPILER_DAEMON): { List<BinaryExpression> expressions ->
                for (BinaryExpression expression : expressions) {
                    if (expression.leftExpression.text.endsWith("options.fork")) {
                        if (expression.rightExpression.text.equals("false")) {
                            return StateFlag.CLOSE
                        } else {
                            return StateFlag.OPEN
                        }
                    }
                }
                return StateFlag.DEFAULT
            },

            (GradleStrategy.GRADLE_INCREMENTAL_COMPILATION): { List<BinaryExpression> expressions ->
                for (BinaryExpression expression : expressions) {
                    if (expression.leftExpression.text.endsWith("options.incremental")) {
                        if (expression.rightExpression.text.equals("false")) {
                            return StateFlag.CLOSE
                        } else {
                            return StateFlag.OPEN
                        }
                    }
                }
                return StateFlag.DEFAULT
            },

            (GradleStrategy.GRADLE_PARALLEL_TEST): { List<BinaryExpression> expressions ->
                for (BinaryExpression expression : expressions) {
                    if (expression.leftExpression.text.equals("maxParallelForks")) {
                        if (expression.rightExpression.text.equals("1")) {
                            return StateFlag.CLOSE
                        } else {
                            return StateFlag.OPEN
                        }
                    }
                }
                return StateFlag.DEFAULT
            },

            (GradleStrategy.GRADLE_FORK_TEST): { List<BinaryExpression> expressions ->
                for (BinaryExpression expression : expressions) {
                    if (expression.leftExpression.text.equals("forkEvery"))
                        if (expression.rightExpression.text.equals("0") || expression.rightExpression.text.startsWith('-')) {
                            return StateFlag.CLOSE
                        } else {
                            return StateFlag.OPEN
                        }
                }
                return StateFlag.DEFAULT
            },

            (GradleStrategy.GRADLE_REPORT_GENERATION): { List<BinaryExpression> expressions ->
                for (BinaryExpression expression : expressions) {
                    if (expression.leftExpression.text.contains("html.required") || expression.leftExpression.text.contains("junitXml.required")) {
                        if (expression.rightExpression.text.equals("true")) {
                            return StateFlag.CLOSE
                        } else {
                            return StateFlag.OPEN
                        }
                    }
                }
                return StateFlag.DEFAULT
            }
    ]

    static StateFlag check(String repoPath, GradleStrategy strategy) {
        List<String> buildFilePaths = Util.getGradleFilePaths(repoPath)
        if (buildFilePaths.size() == 0){
            println("没有找到 .gradle 文件")
            return null
        }
        for (String buildFilePath : buildFilePaths) {
            def flag = singleFileCheck(buildFilePath, strategy)
            if(flag != StateFlag.DEFAULT){
                println("${strategy} 在${buildFilePaths}检测出${flag.toString()}")
                return flag
            }
        }
        return StateFlag.DEFAULT
    }

    static StateFlag singleFileCheck(String buildFilePath, GradleStrategy strategy){
        def predicate = predicatesMap.get(strategy)
        GradleParser parser = new GradleParser(buildFilePath)
        GradleVisitor visitor = parser.getVisitor()
        List<BinaryExpression> expressions = visitor.binaryExpressions
        return predicate.call(expressions)
    }

    static StateFlag check(Repository repository, GradleStrategy strategy) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        return check(repoPath,strategy)
    }

    static void check(GradleStrategy strategy) {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction()
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool == 2) {
                    try{
                        println("处理${repository.getRepoName()}")
                        StateFlag flag = check(repository, strategy)
                        if (flag == null){
                            // repository.setBuildTool(0)  如果没有检测到".gradle"文件
                            continue
                        }
                        if (strategy == GradleStrategy.GRADLE_PARALLEL_TEST) {
                            repository.setParallelTest(flag.getValue())
                        } else if (strategy == GradleStrategy.GRADLE_FORK_TEST) {
                            repository.setGradleForkTest(flag.getValue())
                        } else if (strategy == GradleStrategy.GRADLE_REPORT_GENERATION) {
                            repository.setGradleReportGeneration(flag.getValue())
                        } else if (strategy == GradleStrategy.GRADLE_COMPILER_DAEMON) {
                            repository.setGradleCompilerDaemon(flag.getValue())
                        } else if (strategy == GradleStrategy.GRADLE_INCREMENTAL_COMPILATION) {
                            repository.setGradleIncrementalCompilation(flag.getValue())
                        }
                    }
                    catch (Exception e) {
                        println("error repository_id: " + repository.getId())
                        e.printStackTrace()
                    }
                }
            }
            tx.commit()
        }
    }
}
