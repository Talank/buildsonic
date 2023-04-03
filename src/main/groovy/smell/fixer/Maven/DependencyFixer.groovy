package smell.fixer.Maven

import maven.Dependency
import maven.POM
import maven.POMTree
import model.Repository
import parser.MavenLogParser
import smell.MavenFixer

import java.nio.file.Paths

class DependencyFixer {
    static Map<String, String> getVersionUpdateMap(List<Dependency> dependencies) {
        Map<String, String> map = new LinkedHashMap<>()
        for (Dependency dependency : dependencies) {
            String metaDataPath = Paths.get(System.getProperty("user.dir"), "resources", "dynamicVersion", "${dependency.groupID}@${dependency.artifactID}.xml").normalize().toString();
            File file = new File(metaDataPath)
            if (file.exists()) {
                POMFixer xmlParser = new POMFixer(metaDataPath)
                String newVersion = xmlParser.getLatestReleaseVersion()
                if (!newVersion.equals('')) {
                    map.put(dependency.getVersion(), newVersion)
                }
            }
        }
        return map
    }

    static boolean dynamicVersionFixer(String repoPath) {
        def (pomTree, pom) = MavenFixer.getPomTree(repoPath)
        if (pom == null)
            return false

        List<Dependency> dependencies = pomTree.getDynamicDependencies();
        if (dependencies.size() == 0)
            return false
        Map<String, String> map = getVersionUpdateMap(dependencies)
        if (map.size() == 0)
            return false

        List<String> pomFilePaths = pomTree.getPathList()
        for (String pomFilePath : pomFilePaths) {
            File file = new File(pomFilePath)
            String content = file.text
            boolean flag = false
            map.each {key, value ->
                if (content.contains(key)) {
                    println(pomFilePath)
                    flag = true
                    content = content.replaceAll(key, value)
                    println(content)
                }
            }
            if (flag) {
                file.text = content
            }
        }

        return true
    }

    static boolean dynamicVersionFixer(Repository originRepository, String forkRepoName, String reposDir) {
        if (!originRepository.multiModule)
            return
        String repoPath = Paths.get(reposDir, forkRepoName.replace('/', '@'))
        return dynamicVersionFixer(repoPath)
    }

    static boolean unusedDependeniesFixer(String repoPath, String originRepoName) {
        String originRepoPath = Paths.get(System.getProperty("user.dir"), "resources", "mavenUnusedDependencies", "${originRepoName.replace('/', '@')}.txt").normalize().toString();
        def (buildSuccess, containUnusedDependency, logDependenciesMap) = parser.MavenLogParser.parse(originRepoPath)
        if (!containUnusedDependency)
            return false
        POMTree pomTree = new POMTree(repoPath);
        POM rootPom = null
        try {
            rootPom = pomTree.createPomTree()
        } catch(Exception exception) {
            exception.printStackTrace()
        }
        if (rootPom == null)
            return false

        boolean result = false
        List<POM> poms = pomTree.getPomList()
        for (POM pom : poms) {
            Set<MavenLogParser.LogDependency> logDependencies = new LinkedHashSet<>()
            if (logDependenciesMap.get(pom.getName()) != null)
                logDependencies.addAll(logDependenciesMap.get(pom.getName()))
            if (logDependenciesMap.get(pom.getArtifactId()) != null)
                logDependencies.addAll(logDependenciesMap.get(pom.getArtifactId()))
            boolean flag = false
            POMFixer xmlParser = new POMFixer(pom.getPath())
            if (logDependencies != null && logDependencies.size() > 0) {
                for (MavenLogParser.LogDependency logDependency : logDependencies) {
                    println(pom.getPath())
                    println("g: " + logDependency.getGroupId() + " a:" + logDependency.getArtifactId())
                    flag = xmlParser.removeDependency(logDependency.getGroupId(), logDependency.getArtifactId()) || flag
                }
            }
            if (flag) {
                xmlParser.printToFile(pom.getPath())
            }
            result = flag || result
        }
        return result
    }

    static boolean unusedDependeniesFixer(Repository originRepository, String forkRepoName, String reposDir) {
        if (!originRepository.multiModule)
            return
        String repoPath = Paths.get(reposDir, forkRepoName.replace('/', '@'))
        return unusedDependeniesFixer(repoPath, originRepository.getRepoName())
    }
}
