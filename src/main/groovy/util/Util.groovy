package util

import groovy.io.FileType
import model.Repository
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Util {
    public static final String codeDirectoryPath = Paths.get(System.getProperty("user.dir"), "..", "sequence", "repository").normalize().toString();
    public static final String forkDirectoryPath = Paths.get(System.getProperty("user.dir"), "..", "sequence", "fork").normalize().toString();
    // PRDetailPath：存放"PR详细信息汇总.xlsx"的地址
    public static final String PRDetailPath = Paths.get(System.getProperty("user.dir"), "resources", "PR详细信息汇总.xlsx").normalize().toString()
    // PRDetailFilteredPath: 存放"筛选PR详细信息汇总.xlsx"的地址
    public static final String PRDetailFilteredPath = Paths.get(System.getProperty("user.dir"), "resources", "PR统计信息.xlsx").normalize().toString()
    // codeZipPath: 存放需要触发构建的repo的code ZIP ;  codeUnzipPath: 存放需要触发构建的repo的code.zip解压后的文件
    public static final String codeZipPath = Paths.get(System.getProperty("user.dir"), "..", "sequence", "codeZip").normalize().toString();
    public static final String codeUnzipPath = Paths.get(System.getProperty("user.dir"), "..", "sequence", "codeUnzip").normalize().toString()
    // gitDepthTestPath:测试git depth的路径
    public static final String gitDepthTestPath = Paths.get(System.getProperty("user.dir"), "..", "sequence", "gitDepth").normalize().toString()
    public static final String pullRequestJson = Paths.get(System.getProperty("user.dir"), "resources", "pullRequest@json").normalize().toString()

    static String getPullRequestListFilePath(def o) {
        Paths.get(System.getProperty("user.dir"), "resources", "pullRequest", "${o.toString()}.txt").normalize().toString();
    }

    static String getTravisAPIInfoPath(def strategy){
        // TravisAPIInfoPath: 存放travis API的json数据
        Paths.get(System.getProperty("user.dir"), "resources", "travisAPIInfo", "${strategy.toString()}.json").normalize().toString()
    }

    static String getTriggerRepoPath(def strategy){
        // 测试该strategy的trigger repo路径
        if (strategy instanceof TravisUtil.TravisStrategy){
            return Paths.get(System.getProperty("user.dir"), "..", "${strategy.toString()}_Trigger").normalize().toString()
        }
        if (strategy instanceof GradleUtil.GradleStrategy){
            def category = GradleUtil.getGradleCategory(strategy)
            if (category == GradleUtil.GradleCategory.PROPERTIES){
                return Paths.get(System.getProperty("user.dir"), "..", "GradlePropertyTrigger").normalize().toString()
            }else{
                return Paths.get(System.getProperty("user.dir"), "..", "GradleBuildTrigger").normalize().toString()
            }
        }
        if (strategy instanceof MavenUtil.MavenStrategy){
            def category = MavenUtil.getMavenCategory(strategy)
            if (category == MavenUtil.MavenCategory.TEST || category == MavenUtil.MavenCategory.FORK){
                return Paths.get(System.getProperty("user.dir"), "..", "MavenTestTrigger").normalize().toString()
            }else{
                return Paths.get(System.getProperty("user.dir"), "..", "MavenCompileTrigger").normalize().toString()
            }
        }
       return ""
    }



    static void createDir(String directoryPath) {
        File file = new File(directoryPath)
        if (!file.exists())
            file.mkdir()
    }

    static List<String> splitFullRepoName(String fullRepoName) {
        int index = fullRepoName.indexOf('/')
        return [fullRepoName[0..<index], fullRepoName[index+1..-1]]
    }

    def static retry(int times = 5, Closure errorHandler = {e -> e.printStackTrace}
              , Closure body) {
        int retries = 0
        def exceptions = []
        while(retries++ < times) {
            try {
                return body.call()
            } catch(e) {
                exceptions << e
                errorHandler.call(e)
                sleep(5000)
            }
        }
        throw new Exception("Failed after $times retries", exceptions)
    }

    static String getBuildFileName(String repoPath) {
        String travisConfigFileName = null
        def fileList = []
        new File(repoPath).eachFile(FileType.FILES) {
            fileList << it.name
        }
        fileList.each {
            println(it)
            if (it.equals("pom.xml") || it == "build.gradle") {
                travisConfigFileName = it
                return
            }
        }
        return travisConfigFileName;
    }

    //找到项目中的所有shell文件
    static List<String> getShellFilePaths(String repoPath) {
        List<String> list = []
        File dir = new File(repoPath)
        if (dir.exists()) {
            dir.eachFileRecurse(FileType.FILES) {
                String filePath = it.path
                if (filePath.endsWith(".sh")) {
                    //println(filePath)
                    list << filePath
                }
            }
        }
        return list
    }

    //找到项目中的所有.gradle文件
    static List<String> getGradleFilePaths(String repoPath) {
        List<String> list = []
        File dir = new File(repoPath)
        if (dir.exists()) {
            dir.eachFileRecurse(FileType.FILES) {
                String filePath = it.path
                if (filePath.endsWith(".gradle")) {
                    //println(filePath)
                    list << filePath
                }
            }
        }
        return list
    }

    static void scan() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        for (Repository repository : repositories) {
            String repoName = repository.repoName
            //System.out.println(repository.getId() + " " + repository.getRepoName() + " " + repository.getContainTravisYml());
            String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@')).normalize().toString();
            File file = new File(Paths.get(repoPath, "settings.gradle").toString())
            if (file.exists()) {
                println(repoName)
                println(file.text)
                println("==================")
            }
        }
    }

    static FileBasedConfigurationBuilder<FileBasedConfiguration> getConfigurationBuilder(String path) {
        Parameters params = new Parameters();
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.properties()
                                .setFileName(path));
        return builder
    }

    static void main(String[] args) {
        Util.scan()
    }
}
