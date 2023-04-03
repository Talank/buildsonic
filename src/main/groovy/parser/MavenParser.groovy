package parser

import groovy.io.FileType
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

class MavenParser {
    String repoPath
    MavenParser(String repoPath) {
        this.repoPath = repoPath
    }

    List<String> getPomFiles() {
        def list = []
        new File(repoPath).eachFileRecurse(FileType.FILES) {
            if (it.toString().endsWith("pom.xml")) {
                list << it.toString()
            }
        }
        return list
    }

    def getPomModel(String pomFilePath) {
        MavenXpp3Reader reader = new MavenXpp3Reader()
        Model model = null;
        FileReader fr = new FileReader(pomFilePath)
        model = reader.read(fr);
        fr.close()
        model.setPomFile(new File(pomFilePath));
        return model;
    }

    void run() {
        List<String> pomFilePaths = getPomFiles()
        println(pomFilePaths)
        for (pomFilePath in pomFilePaths) {
            Model pomModel = getPomModel(pomFilePath)
            for (dep in pomModel.getProfiles()) {
                println(dep)
            }
            println("====")
        }
    }

    static void main(String[] args) {
        new MavenParser("/Users/zhangchen/projects/repos/apache@pdfbox").run()
    }
}
