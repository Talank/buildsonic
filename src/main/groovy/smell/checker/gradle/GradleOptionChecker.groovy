package smell.checker.gradle

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import parser.OptionParser
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths
import smell.StateFlag
import static util.GradleUtil.GradleStrategy

class GradleOptionChecker {

    static StateFlag parallelExecutionChecker(List<String> yml_shell_commands, Map<String, String> gradleProperties) {
        if (gradleProperties.get("org.gradle.parallel") == "true" || yml_shell_commands.any{it.contains("--parallel") || (it =~ /org.gradle.parallel\s*=\s*true/)})
            return StateFlag.OPEN

        if (gradleProperties.get("org.gradle.parallel") == "false" || yml_shell_commands.any{it.contains("--no-parallel") || (it =~ /org.gradle.parallel\s*=\s*false/)})
            return StateFlag.CLOSE

        return StateFlag.DEFAULT
    }

    static StateFlag fileSystemWatchingChecker(List<String> yml_shell_commands, Map<String, String> gradleProperties) {
        if (gradleProperties.get("org.gradle.vfs.watch") == "true" || yml_shell_commands.any{it.contains("--watch-fs") || (it =~ /org.gradle.vfs.watch\s*=\s*true/)})
            return StateFlag.OPEN

        if (gradleProperties.get("org.gradle.vfs.watch") == "false" || yml_shell_commands.any{it.contains("--no-watch-fs") || (it =~ /org.gradle.vfs.watch\s*=\s*false/)})
            return StateFlag.CLOSE

        return StateFlag.DEFAULT
    }

    static StateFlag configureOnDemandChecker(List<String> yml_shell_commands, Map<String, String> gradleProperties) {
        if (gradleProperties.get("org.gradle.configureondemand") == "true" || yml_shell_commands.any{it.contains("--configure-on-demand") || (it =~ /org.gradle.configureondemand\s*=\s*true/)})
            return StateFlag.OPEN

        if (gradleProperties.get("org.gradle.configureondemand") == "false" || yml_shell_commands.any{it.contains("--no-configure-on-demand") || (it =~ /org.gradle.configureondemand\s*=\s*false/)})
            return StateFlag.CLOSE

        return StateFlag.DEFAULT
    }

    static StateFlag cacheChecker(List<String> yml_shell_commands, Map<String, String> gradleProperties) {
        if (gradleProperties.get("org.gradle.caching") == "true" || yml_shell_commands.any{it.contains("--build-cache") || (it =~ /org.gradle.caching\s*=\s*true/)})
            return StateFlag.OPEN

        if (gradleProperties.get("org.gradle.caching") == "false" || yml_shell_commands.any{it.contains("--no-build-cache") || (it =~ /org.gradle.caching\s*=\s*false/)})
            return StateFlag.CLOSE

        return StateFlag.DEFAULT
    }

    static StateFlag daemonChecker(List<String> yml_shell_commands, Map<String, String> gradleProperties) {
        if (gradleProperties.get("org.gradle.daemon") == "true" || yml_shell_commands.any{it.contains("--daemon") || (it =~ /org.gradle.daemon\s*=\s*true/)})
            return StateFlag.OPEN

        if (gradleProperties.get("org.gradle.daemon") == "false" || yml_shell_commands.any{it.contains("--no-daemon") || (it =~ /org.gradle.daemon\s*=\s*false/)})
            return StateFlag.CLOSE

        return StateFlag.DEFAULT
    }

    static StateFlag gradleChecker(String repoPath, String originRepoName, GradleStrategy strategy) {
        OptionParser optionParser = new OptionParser(repoPath, originRepoName)
        def (yamlGradleCommands, shellGradleCommands, gradleProperties) = optionParser.getGradleOptions()
        List<String> yml_shell_commands = yamlGradleCommands + shellGradleCommands
        if (strategy == GradleStrategy.PARALLEL_BUILDS) {
            return parallelExecutionChecker(yml_shell_commands, gradleProperties)
        } else if (strategy == GradleStrategy.FILE_SYSTEM_WATCHING) {
            return fileSystemWatchingChecker(yml_shell_commands, gradleProperties)
        } else if (strategy == GradleStrategy.CONFIGURATION_ON_DEMAND) {
            return configureOnDemandChecker(yml_shell_commands, gradleProperties)
        } else if (strategy == GradleStrategy.CACHING) {
            return cacheChecker(yml_shell_commands, gradleProperties)
        } else if (strategy == GradleStrategy.GRADLE_DAEMON) {
            return daemonChecker(yml_shell_commands, gradleProperties)
        }
    }
    //是否配置并行执行策略
    static void gradleChecker(GradleStrategy strategy) {
        try (Session session = SessionUtil.getSession()) {
            Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool == 2) {
                    StateFlag flag = gradleChecker(repository, strategy)
                    println("处理${repository.getRepoName()}")
                    if (strategy == GradleStrategy.PARALLEL_BUILDS) {
                        repository.setParallelExecution(flag.getValue())
                    } else if (strategy == GradleStrategy.FILE_SYSTEM_WATCHING) {
                        repository.setFileSystemWatch(flag.getValue())
                    } else if (strategy == GradleStrategy.CONFIGURATION_ON_DEMAND) {
                        repository.setConfigureOnDemand(flag.getValue())
                    } else if (strategy == GradleStrategy.CACHING) {
                        repository.setGradleCache(flag.getValue())
                    } else if (strategy == GradleStrategy.GRADLE_DAEMON) {
                        repository.setGradleDaemon(flag.getValue())
                    }
                }

            }
            tx.commit()
        }
    }

    static StateFlag offlineChecker(Repository repository) {
        Map<String, Object> options = new OptionParser(repository).getOptions()
        List<String> yml_shell = options.get(OptionParser.FileType.YAML) +  options.get(OptionParser.FileType.SHELL)

        for (String command : yml_shell) {
            if (command.contains("gradle") && command.contains("--offline")) {
                println(command)
                return StateFlag.OPEN
            }
        }

        return StateFlag.DEFAULT
    }

    //daemon
    static void offlineChecker() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        Map<StateFlag, Integer> map = new HashMap<>()
        for (Repository repository : repositories) {
            if (repository.buildTool != 2)
                continue
            StateFlag flag = offlineChecker(repository)
            if (flag == StateFlag.OPEN) {
                map[StateFlag.OPEN] = map.getOrDefault(StateFlag.OPEN, 0) + 1
                println(repository.repoName)
            }
            else if (flag == StateFlag.CLOSE) {
                map[StateFlag.CLOSE] = map.getOrDefault(StateFlag.CLOSE, 0) + 1
                println(repository.repoName + " " + flag)
            } else if (flag == StateFlag.DEFAULT)
                map[StateFlag.DEFAULT] = map.getOrDefault(StateFlag.DEFAULT, 0) + 1
        }
        println(map)
    }

    static void main(String[] args) {
        //gradleChecker("parallel_execution")
        //gradleChecker("file_system_watch")
        //gradleChecker("configure_on_demand")
        //gradleChecker("gradle_cache")
        gradleChecker(GradleStrategy.GRADLE_DAEMON)
    }
}
