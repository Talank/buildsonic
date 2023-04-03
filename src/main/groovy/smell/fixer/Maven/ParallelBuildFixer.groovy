package smell.fixer.Maven

import model.Repository
import parser.OptionParser
import smell.StateFlag
import smell.checker.gradle.GradleOptionChecker
import smell.checker.maven.ParallelBuildChecker

import java.nio.file.Paths

class ParallelBuildFixer {
    static String parallelExecutionFixerFile(String filePath) {
        File file = new File(filePath)
        String content = file.text
        content = content.replaceAll('mvn ', 'mvn -T 1C ')
        content = content.replaceAll('mvnw ', 'mvnw -T 1C ')
        return content
    }

    static List<String> getParallelExecutionEditedPaths(OptionParser optionParser) {
        List<String> result = new ArrayList<>()
        List<String> yml_shell = optionParser.getYamlMavenBuildCommands()
        if (yml_shell.any {it.contains("mvn")}) {
            String ymlFilePath = Paths.get(optionParser.repoPath, ".travis.yml")
            result.add(ymlFilePath)
        } else {
            Map<String, List<String>> map = optionParser.getShellBuildCommandsMap()
            map.each {key, list ->
                if (list.any {it.contains("mvn")}) {
                    String shellFilePath = Paths.get(optionParser.repoPath, key)
                    result.add(shellFilePath)
                }
            }
        }
        return result
    }

    static boolean parallelExecutionFixer(String repoPath, String originRepoName) {
        OptionParser optionParser = new OptionParser(repoPath, originRepoName)
        //如果项目已经开启了并行构建就返回
        if (ParallelBuildChecker.parallelExecutionChecker(optionParser) == StateFlag.OPEN)
            return false
        List<String> yml_shell = optionParser.getYamlMavenBuildCommands() + optionParser.getShellBuildCommands()
        if (!yml_shell.any {it.contains("mvn")})
            return false

        List<String> filePaths = getParallelExecutionEditedPaths(optionParser)
        filePaths.each {
            String content = parallelExecutionFixerFile(it)
            new File(it).text = content
        }
        return true
    }

    static boolean parallelExecutionFixer(Repository originRepository, String forkRepoName, String reposDir) {
        if (!originRepository.multiModule)
            return false
        String repoPath = Paths.get(reposDir, forkRepoName.replace('/', '@'))
        return parallelExecutionFixer(repoPath, originRepository.repoName)
    }
}
