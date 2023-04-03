package smell

import model.Repository
import parser.GradleParser
import parser.GradleVisitor
import static util.GradleUtil.GradleStrategy
import util.MysqlUtil
import util.Util

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class GradleFixer {
    static String parallelTestPatch = '''
tasks.withType(Test).configureEach {
    maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1
}
'''\

    static boolean gradlePropertiesFixer(String filePath) {
        File file = new File(filePath)
        if (!file.exists()) {
            return false
        }
        println(filePath)
        println(file.text)
    }

    static void gradlePropertiesFixer() {
        List<Repository> repositories = MysqlUtil.getRepositories()
        for (Repository repository : repositories) {
            if (repository.buildTool == 2) {
                String path = Paths.get(Util.codeDirectoryPath.toString(), repository.getRepoName().replace('/', '@'), "gradle.properties").normalize().toString();
                gradlePropertiesFixer(path)
            }
        }
    }

    static int getInsertLineNumber() {

    }

    static boolean gradleFileFixer(String filePath, GradleStrategy strategy, String outPath = null) {
        if (outPath == null)
            outPath = filePath
        GradleParser parser = new GradleParser(filePath)
        GradleVisitor visitor = parser.getVisitor()
        File file = new File(filePath)
        List<String> lines = file.readLines()
        Pattern pattern = ~/(.*)(}.*)/
        int lineNum = lines.size()
        println(lineNum)
        if (visitor.allprojectsLastLineNumber != 0) {
            lineNum = visitor.allprojectsLastLineNumber - 1
        } else if (visitor.subprojectsLastLineNumber != 0) {
            lineNum = visitor.subprojectsLastLineNumber - 1
        }
        if (lineNum == lines.size()) {
            lines[lineNum] = parallelTestPatch
        } else {
            String line = lines[lineNum]
            Matcher matcher = pattern.matcher(line)
            matcher.replaceFirst('$1\n' + parallelTestPatch + '$2')
            lines[lineNum] = line
        }

        new File(outPath).text = lines.join('\n') + '\n'
    }

    static void main(String[] args) {
        gradlePropertiesFixer()
    }
}
