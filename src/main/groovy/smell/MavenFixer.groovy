package smell


import maven.POM
import maven.POMTree
import model.Repository
import smell.fixer.Maven.TestFixer
import util.MysqlUtil
import util.Util

import java.nio.file.Paths

class MavenFixer {
    static def getPomTree(String repoPath) {
        POMTree pomTree = new POMTree(repoPath);
        POM pom = pomTree.createPomTree()
        return [pomTree, pom]
    }

    static void main(String[] args) {
        List<Repository> repositories = MysqlUtil.getRepositories()
        for (Repository repository : repositories) {
            if (repository.buildTool == 1 && repository.parallelTest == false) {
                String repoPath = Paths.get(Util.codeDirectoryPath, repository.repoName.replace('/', '@'))
                //println(repoPath)
                try{
                    TestFixer.fixer(repoPath)
                } catch(Exception e) {
                    //e.printStackTrace()
                }
            }
        }
    }
}
