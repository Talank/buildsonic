package parser

import model.Repository
import org.yaml.snakeyaml.Yaml
import util.MysqlUtil
import util.Util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/*
====
Fast Finish:

 */
class TravisParser {

    static String readYml(Path ymlFilePath) {
        String content = null;
        try {
            content = new String(Files.readAllBytes(ymlFilePath));
        } catch (IOException e) {
             //e.printStackTrace();
        }
        return content;
    }

    static Map<String, Object> parseYml(String content, String repoName) {
        Yaml yaml = new Yaml();
        Map<String, Object> result = null;
        try {
            result = yaml.load(content);
        } catch (Exception e) {
            //System.out.println(ymlFilePath);
            //e.printStackTrace();
        }
        //System.out.println(result);
        return result;
    }

    static void checkGitDepth(List<Map<String, Object>> results) {
        def triplet = [falseNum: 0, bigNum: 0, lessNum: 0]
        for (Map<String, Object> result : results) {
            checkGitDepth(result, triplet);
        }
        System.out.println("falseNum: " + triplet.falseNum + " bigNum: " + triplet.bigNum + " lessNum: " + triplet.lessNum );
    }

    static void checkGitDepth(Map<String, Object> result, LinkedHashMap<String, Integer> triplet) {
        if (result == null || result.containsKey("git") == false)
            return;

        Map<String, Object> git = (Map<String, Object>) result.get("git");
        if (git.get("depth") == null)
            return;
        if (git.get("depth").getClass() == Integer.class) {
            println(git.get("depth"))
            if ((Integer)git.get("depth") > 50) {
                triplet.bigNum += 1;
            } else if ((Integer)git.get("depth") < 50) {
                triplet.lessNum += 1;
            }
        } else if (git.get("depth").getClass() == Boolean.class) {
            triplet.falseNum += 1;
        }
    }

    static void checkTravisRetry(LinkedList<String> results) {
        def count = 0
        for (String result : results) {
            if (result.contains("travis_retry") || result.contains("--retry")) {
                println(result)
                count++;
            }
        }
        println(count)
    }

    static void checkTravisWait(LinkedList<String> results) {
        def count = 0
        for (String result : results) {
            if (result.contains("travis_wait")) {
                println(result)
                count++;
            }
        }
        println(count)
    }

    static void checkCache(Map<String, Map<String, Object>> results) {
        def count = 0
        results.each {key, value ->
            if (value != null && value.containsKey("cache")) {
                count++
                println(value["cache"])
            }
        }
        System.out.println(count);
    }

    static void checkEnv(Map<String, Map<String, Object>> results, Map<String, String> contents) {
        for (result in results) {
            if (result.value != null && result.value.containsKey("env")) {
                println(result.key)
                println(contents[result.key])
                println()
            }
        }
    }

    static void checkFastFinish(Map<String, String> contents) {
        int count = 0
        for (m in contents) {
            if (m.value.contains("allow_failures") && (m.value.contains("fast_finish") == false) ) {
                count++
                println(m.key)
            }
        }
        println(count)
    }

    static void run() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        Map<String, Map<String, Object>> results = [:];
        Map<String, String> contents = [:];
        for (Repository repository : repositories) {
            String repoName = repository.repoName
            //System.out.println(repository.getId() + " " + repository.getRepoName() + " " + repository.getContainTravisYml());
            Path ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize();
            String content = readYml(ymlFilePath);
            if (content == null)
                continue
            //System.out.println(ymlFilePath);
            Map<String, Object> result = parseYml(content, repository.getRepoName());
            contents.put(repoName, content)
            results.put(repoName, result);
        }
        //checkGitDepth(results);
        //checkTravisRetry(contents);
        // checkTravisWait(contents)
        checkCache(results)
        //checkEnv(results, contents)
        // checkFastFinish(contents)

    }

    static void testPomCasche() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        for (Repository repository : repositories) {
            if (repository.buildTool == 1) {
                String repoName = repository.repoName
                Path ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize();
                String content = readYml(ymlFilePath);
                if (content == null)
                    continue
                if (content.contains("cache") == false) {
                    println("\"${repoName}\",")
                }
            }

        }
    }

    static void testShell() {
        List<Repository> repositories = MysqlUtil.getRepositories();
        int count = 1
        println(repositories.size())
        for (Repository repository : repositories) {
            String repoName = repository.repoName
            Path ymlFilePath = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), ".travis.yml").normalize();
            String content = readYml(ymlFilePath);
            if (content == null)
                continue
            if (content.contains(".sh")) {
                //println("\"${repoName}\",")
                count++
            }
            List<String> list = new File(ymlFilePath.toString()).readLines()
            list.each {
                if (it.contains(".sh"))
                    println(it)
            }
        }
        println(count)
    }

    static void main(String[] args) {
        //TravisParser.run();
        //TravisParser.testPomCasche();
        TravisParser.testShell()
    }
}
