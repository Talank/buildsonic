package parser

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.MultipleCompilationErrorsException

import java.nio.file.Files

/*
 https://github.com/pwestlin/gradledependencyparser
 https://github.com/yigitozgumus/gradleParser
 https://github.com/kevinxyz/gradle-dependency-graph
 https://github.com/spyhunter99/gradle-dependencies-to-pom
 https://github.com/lovettli/liferay-ide/blob/master/tools/tests/com.liferay.ide.gradle.core.tests/src/com/liferay/ide/gradle/core/tests/GradleParseTests.java
 https://github.com/quarkusio/quarkus/issues/3497
 https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000699864-Parse-and-Modify-gradle-file
 */
class GradleParser {
    private List<ASTNode> nodes = [];
    private List<String> gradleFileContents;
    GradleVisitor visitor = new GradleVisitor();
    GradleParser(String buildFilePath) throws MultipleCompilationErrorsException, IOException
    {
        String buildFileContent = ""
        if (buildFilePath.endsWith(".gradle"))
            buildFileContent = new File(buildFilePath).text
        if (buildFileContent.size() < 4) {
            return
        }
        AstBuilder builder = new AstBuilder()
        try {
            this.nodes = builder.buildFromString(CompilePhase.CONVERSION, buildFileContent)
        } catch(Exception e){
            e.printStackTrace()
        }
        for( ASTNode node : this.nodes )
        {
            if (node instanceof BlockStatement) {
                node.visit( this.visitor );
            } else if (node instanceof InnerClassNode) {

            }
        }
    }

    GradleVisitor insertDependency( String dependency ) throws IOException
    {
        GradleVisitor visitor = new GradleVisitor();
        walkScript( visitor );
        gradleFileContents = Files.readAllLines( Paths.get( file.toURI() ) );

        if( visitor.getDependenceLineNum() == -1 )
        {
            if( !dependency.startsWith( "\t" ) )
            {
                dependency = "\t" + dependency;;
            }

            gradleFileContents.add( "" );
            gradleFileContents.add( "dependencies {" );
            gradleFileContents.add( dependency );
            gradleFileContents.add( "}" );
        }
        else
        {
            if( visitor.getColumnNum() != -1 )
            {
                gradleFileContents = Files.readAllLines( Paths.get( file.toURI() ) );
                StringBuilder builder = new StringBuilder( gradleFileContents.get( visitor.getDependenceLineNum() - 1 ) );
                builder.insert( visitor.getColumnNum() - 2, "\n" + dependency + "\n" );
                String dep = builder.toString();

                if( CoreUtil.isWindows() )
                {
                    dep.replace( "\n", "\r\n" );
                }
                else if( CoreUtil.isMac() )
                {
                    dep.replace( "\n", "\r" );
                }

                gradleFileContents.remove( visitor.getDependenceLineNum() - 1 );
                gradleFileContents.add( visitor.getDependenceLineNum() - 1, dep );
            }
            else
            {
                gradleFileContents.add( visitor.getDependenceLineNum() - 1, dependency );
            }
        }

        return visitor;
    }

    List<GradleDependency> getAllDependencies()
    {
        GradleVisitor visitor = new GradleVisitor();
        walkScript( visitor );

        return visitor.getDependencies();
    }

    List<String> getGradleFileContents()
    {
        return gradleFileContents;
    }

    static void main(String[] args) {
        File file = new File("/Users/zhangchen/projects/BuildPerformance/build.gradle")
        GradleParser parser = new GradleParser(file)
        List<GradleDependency> dependencies = parser.getAllDependencies()
        dependencies.each {
            println(it)
        }
    }
}
