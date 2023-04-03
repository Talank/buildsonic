package parser

import groovy.json.JsonSlurper
import model.Repository
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

//抽取配置文件关键指令
class OptionParser {
    String repoPath
    //项目的原始名字
    String originRepoName
    Map<String, Object> options = new HashMap<>()

    OptionParser(String repoPath){
        this.repoPath = repoPath
    }
    OptionParser(String repoPath, String originRepoName) {
        this.repoPath = repoPath
        this.originRepoName = originRepoName
    }

    public enum FileType {
        YAML, SHELL, PROPERTIES
    }
    public Map<String, Object> getOptions() {
        if (this.options.size() > 0)
            return this.options
        //抽取yml中的运行指令
        List<String> yml = getYamlBuildCommands()
        options.put(FileType.YAML, yml)
        //抽取shell中的指令
        List<String> shell = getShellBuildCommands()
        options.put(FileType.SHELL, shell)
        //抽取gradle.properties中所设置的属性
        Map<String, String> properties = parseGradleProperties()
        options.put(FileType.PROPERTIES, properties)
        return this.options
    }

    public def getGradleOptions() {
        List<String> yamlGradleCommands = getYamlGradleBuildCommands()
        List<String> shellGradleCommands = getShellBuildCommands()
        shellGradleCommands.removeIf {!it.contains("gradle")}
        Map<String, String> gradleProperties = parseGradleProperties()
        return [yamlGradleCommands,  shellGradleCommands, gradleProperties]
    }

    //解析.travis.yml文件，抽取maven, gradle相关命令
    List<String> getYamlBuildCommands() {
        String ymlFilePath = Paths.get(this.repoPath, ".travis.yml").normalize().toString();
        //println(ymlFilePath)
        LinkedHashMap<String, Object> map = YmlParser.parse(ymlFilePath)
        List<String> commands = YmlParser.getCommands(map)
        List<String> result = []
        commands.each {
            if (it.contains("mvn") || it.contains("gradle"))
                result << it
        }
        return result
    }

    List<String> getYamlMavenBuildCommands() {
        List<String> commands = getYamlBuildCommands()
        commands.removeIf {!it.contains("mvn")}
        return commands
    }

    List<String> getYamlGradleBuildCommands() {
        List<String> commands = getYamlBuildCommands()
        commands.removeIf {!it.contains("gradle")}
        return commands
    }

    //调用python程序分析shell文件
    void extractAndStoreShell() {
        String pyDir = Paths.get(System.getProperty("user.dir"), "py").normalize().toString();
        //println(pyDir)
        List<String> shellFilePaths = Util.getShellFilePaths(this.repoPath)
        if (shellFilePaths.size() == 0)
            return
        List<String> com = ["python", "shell_parser.py"] + shellFilePaths
        //println(com)
        Process process = com.execute(null, new File(pyDir))
        process.waitForProcessOutput(System.out, System.err)
    }

    List<String> getShellBuildCommands() {
        List<String> list= []
        String filePath = Paths.get(System.getProperty("user.dir"), "resources", "shellCommandJson", this.originRepoName.replace('/', '@') + '.json').normalize().toString();
        File file = new File(filePath)
        if (file.exists() == false)
            return list
        def object = new JsonSlurper().parseText(file.text)
        object.each {
            list << it["command"]["command"]
        }
        //println(list)
        return list
    }

    Map<String, List<String>> getShellBuildCommandsMap() {
        Map<String, List<String>> map = new LinkedHashMap<>()
        String filePath = Paths.get(System.getProperty("user.dir"), "resources", "shellCommandJson", this.originRepoName.replace('/', '@') + '.json').normalize().toString();
        File file = new File(filePath)
        if (file.exists() == false)
            return map

        def object = new JsonSlurper().parseText(file.text)
        object.each {
            if (map.get(it['filePath']) == null) {
                map.put(it['filePath'], [])
            }
            map.get(it['filePath']) << it["command"]["command"]
        }
        //println(list)
        return map
    }

    Map<String, String> parseGradleProperties() {
        //Map<String, String>
        String filePath = Paths.get(this.repoPath, "gradle.properties").normalize().toString();
        File file = new File(filePath)
        if (file.exists() == false)
            return [:]
        Properties properties = new Properties()
        file.withInputStream {
            properties.load(it)
        }
        //println(properties)
        return properties
    }

    static void main(String[] args) {
        List<Repository> repositories = MysqlUtil.getRepositories();
        for (Repository repository : repositories) {
            //PropertyParser.extractAndStoreShell(repository)
            //PropertyParser.parseGradleProperty(repository)
            //OptionParser.getShellBuildCommands(repository)
        }
    }
}
