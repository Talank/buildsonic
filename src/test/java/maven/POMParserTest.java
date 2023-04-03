package maven;

import org.junit.jupiter.api.Test;
import util.Util;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class POMParserTest {

    @Test
    void testParse() throws Exception {
        String s = "${dfafa}-${afafavsds}";
        Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            assertEquals("${dfafa}", matcher.group(0));
            assertEquals("dfafa", matcher.group(1));
        }

        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), "miltonio@milton2").normalize().toString();
        POMTree pomTree = new POMTree(repoPath);
        pomTree.createPomTree();
        for (Map.Entry<String, List<Dependency>> i : pomTree.getDependenciesMap().entrySet()) {
            //System.out.println(i.getKey());
            //i.getValue().forEach(System.out::println);
        }
        String modulePath = Paths.get(repoPath, "../moduleName", "pom.xml").normalize().toString();
        //System.out.println(modulePath);

        //System.out.println(pomTree.getRepositoryUrls());

        repoPath = Paths.get(Util.codeDirectoryPath.toString(), "aws@aws-sdk-java").normalize().toString();
        pomTree = new POMTree(repoPath);
        POM pom = pomTree.createPomTree();
        for (Map.Entry<String, List<Dependency>> i : pomTree.getDependenciesMap().entrySet()) {
            //System.out.println(i.getKey());
            //i.getValue().forEach(System.out::println);
        }
        //System.out.println(pomTree.getRepositoryUrls());
        System.out.println(pom.getTestConfigurations());
    }


}