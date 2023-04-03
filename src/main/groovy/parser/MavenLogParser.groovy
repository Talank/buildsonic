package parser

import model.Repository
import org.hibernate.Session
import util.MysqlUtil
import util.SessionUtil

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class MavenLogParser {
    static class LogDependency {
        String groupId
        String artifactId

        LogDependency(String groupId, String artifactId) {
            this.groupId = groupId
            this.artifactId = artifactId
        }

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            LogDependency that = (LogDependency) o

            if (artifactId != that.artifactId) return false
            if (groupId != that.groupId) return false

            return true
        }

        int hashCode() {
            int result
            result = (groupId != null ? groupId.hashCode() : 0)
            result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0)
            return result
        }

        @Override
        public String toString() {
            return "{" +
                    "groupId='" + groupId + '\'' +
                    ", artifactId='" + artifactId + '\'' +
                    '}';
        }
    }

    static def parse(String logFilePath) {
        File file = new File(logFilePath)
        if (!file.exists())
            return [false, false, null]
        List<String> lines = file.readLines()
        boolean isSuccess = lines.any {it.contains("BUILD SUCCESS")}
        if (!isSuccess) {
            return [isSuccess, false, null]
        }


        Map<String, String> depsMap = new LinkedHashMap<>()
        List<String> depList;
        Pattern pattern = ~/maven-dependency-plugin:.+:analyze.+@ (.+) ---/
        boolean flag = false
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line)
            if (matcher.find()) {
                depList = new ArrayList<>()
                depsMap.put(matcher.group(1), depList)
            }
            if (line.contains("Unused declared dependencies found:"))
                flag = true
            if (flag) {
                //println(line)
                depList << line
            }
            if (flag && !line.contains(":"))
                flag = false
        }

        Map<String, List<LogDependency>> logDependenciesMap = new LinkedHashMap<>()
        depsMap.each {key, list ->
            List<LogDependency> logDependencies = new ArrayList<>();
            logDependenciesMap.put(key, logDependencies)
            for (String line : list) {
                line = line.replaceAll('\\[WARNING\\]', '')
                line = line.trim()
                println(line)
                String[] strings = line.split(":")
                if (strings.size() > 1 && !strings[1].contains('lombok') && !strings[1].contains('junit-jupiter')) {
                    LogDependency logDependency = new LogDependency(strings[0], strings[1])
                    logDependencies << logDependency
                }
            }
        }
        int size = 0
        logDependenciesMap.each {k, v ->
            size += v.size()
        }
        if (size == 0) {
            return [isSuccess, false, null]
        } else {
            return [isSuccess, true, logDependenciesMap]
        }
    }

    public static run() {
        try (Session session = SessionUtil.getSession()) {
            //Transaction tx = session.beginTransaction();
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            for (Repository repository : repositories) {
                if (repository.buildTool != 1)
                    continue
                println(repository.getRepoName())
                String path = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "${repository.getRepoName().replace('/', '@')}.txt").normalize().toString();
                println(path)
                def (buildSuccess, contain_unused_dependency, logDependencies) = parse(path)
                repository.setBuildSuccess(buildSuccess)
                repository.setContainUnusedDependency(contain_unused_dependency)
            }
            //tx.commit()
        }
    }

    static void main(String[] args) {
        run()
    }
}
