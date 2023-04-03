package smell.checker.maven

import model.Repository
import parser.OptionParser
import smell.StateFlag
import util.GradleUtil
import util.MysqlUtil

class OfflineChecker {
    static StateFlag offlineChecker(Repository repository) {
        Map<String, Object> options = new OptionParser(repository).getOptions()

        List<String> yml_shell = options.get(OptionParser.FileType.YAML) +  options.get(OptionParser.FileType.SHELL)

        for (String command : yml_shell) {
            if (command.contains("mvn") && command.contains("curl") == false && command =~ /mvn.*\s-o/) {
                println(command)
                println("=======")
                return GradleUtil.StateFlag.OPEN
            }
        }


        return GradleUtil.StateFlag.DEFAULT
    }

    static void offlineChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<StateFlag, Integer> map = new HashMap<>()
        for (Repository repository : repositories) {
            if (repository.buildTool != 1)
                continue
            StateFlag flag = offlineChecker(repository)
            if (flag == StateFlag.OPEN)
                map[StateFlag.OPEN] = map.getOrDefault(StateFlag.OPEN, 0) + 1
            else if (flag == StateFlag.CLOSE) {
                map[StateFlag.CLOSE] = map.getOrDefault(StateFlag.CLOSE, 0) + 1
                println(repository.repoName + " " + flag)
            } else if (flag == StateFlag.DEFAULT)
                map[StateFlag.DEFAULT] = map.getOrDefault(StateFlag.DEFAULT, 0) + 1
        }
        println(map)
    }
}
