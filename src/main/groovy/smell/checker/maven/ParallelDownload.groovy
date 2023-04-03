package smell.checker.maven

import model.Repository
import parser.OptionParser
import smell.StateFlag
import util.GradleUtil
import util.MysqlUtil

class ParallelDownload {
    static StateFlag parallelDownloadChecker(Repository repository) {
        Map<String, Object> options = new OptionParser(repository).getOptions()

        List<String> yml_shell = options.get(OptionParser.FileType.YAML) +  options.get(OptionParser.FileType.SHELL)

        for (String command : yml_shell) {
            if (command.contains("maven.artifact.threads")) {
                println(command)
                return GradleUtil.StateFlag.OPEN
            }
        }


        return StateFlag.DEFAULT
    }

    static void parallelDownloadChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<StateFlag, Integer> map = new HashMap<>()
        for (Repository repository : repositories) {
            if (repository.buildTool != 1)
                continue
            StateFlag flag = parallelDownloadChecker(repository)
            if (flag == GradleUtil.StateFlag.OPEN)
                map[StateFlag.OPEN] = map.getOrDefault(GradleUtil.StateFlag.OPEN, 0) + 1
            else if (flag == GradleUtil.StateFlag.CLOSE) {
                map[StateFlag.CLOSE] = map.getOrDefault(GradleUtil.StateFlag.CLOSE, 0) + 1
                println(repository.repoName + " " + flag)
            } else if (flag == GradleUtil.StateFlag.DEFAULT)
                map[StateFlag.DEFAULT] = map.getOrDefault(GradleUtil.StateFlag.DEFAULT, 0) + 1
        }
        println(map)
    }
}
