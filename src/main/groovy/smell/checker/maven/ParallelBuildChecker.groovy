package smell.checker.maven

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import parser.OptionParser
import smell.StateFlag
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class ParallelBuildChecker {
    static Pattern pattern = ~/ -T\s+(\S+)/
    static StateFlag parallelExecutionChecker(OptionParser optionParser) {
        List<String> yml_shell = optionParser.getYamlMavenBuildCommands() +  optionParser.getShellBuildCommands()
        StateFlag flag = StateFlag.DEFAULT
        for (String command : yml_shell) {
            if (command.contains("mvn") && command.contains(" -T ")) {
                println(command)
                Matcher matcher = pattern.matcher(command)
                if (matcher.find()) {
                    String num = matcher.group(1)
                    println('==========')
                    println(num)
                    if (num.equals('1')) {
                        flag = StateFlag.CLOSE
                    } else {
                        flag = StateFlag.OPEN
                        break
                    }
                }
            }
        }
        return flag
    }

    static StateFlag parallelExecutionChecker(String repoPath, String originRepoName) {
        OptionParser optionParser = new OptionParser(repoPath, originRepoName)
        return parallelExecutionChecker(optionParser)
    }

    static StateFlag parallelExecutionChecker(Repository repository) {
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
        return  parallelExecutionChecker(repoPath, repository.repoName)
    }

    static void parallelExecutionChecker() {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool != 1)
                    continue
                println(repository.repoName)
                StateFlag flag = parallelExecutionChecker(repository)
                repository.setParallelExecution(flag.getValue())
            }
            tx.commit()
        }
    }
}
