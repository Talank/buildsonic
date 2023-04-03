import org.apache.maven.model.*;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.shared.invoker.*;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class HandleMaven {
    public boolean handleyml(String filepath) throws FileNotFoundException {
        File f = new File(filepath);
        InputStream in = new FileInputStream(f);
        Yaml yaml = new Yaml();
        Map<String, Object> ret = (Map<String, Object>) yaml.load(in);
        String yamlString=new String();
        for (Map.Entry entry:ret.entrySet()){
            yamlString+=entry.getKey().toString()+entry.getValue().toString();
        }
        boolean result = Pattern.matches("mvn -T|mvn -Dmaven.artifact.threads",yamlString);
        return result;
    }

    public boolean handlepom(String completePath){
        //添加maven-dependency-plugin插件
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            FileWriter writer = new FileWriter("out.txt");;
            FileReader fr = new FileReader(completePath);
            model = reader.read(fr);
            Build build=model.getBuild();
            Plugin dependencyPlugin=new Plugin();
            dependencyPlugin.setGroupId("org.apache.maven.plugins");
            dependencyPlugin.setArtifactId("maven-dependency-plugin");
            dependencyPlugin.setVersion("2.8");
            PluginExecution excuteAnalyse=new PluginExecution();
            //excuteAnalyse.setPhase("compile");
            excuteAnalyse.addGoal("tree");
            excuteAnalyse.addGoal("analyze");
            /*String configString=
            Xpp3Dom config = null;
            config = Xpp3DomBuilder.build(new StringReader(configString.toString()));*/
            //excuteAnalyse.setConfiguration();
            dependencyPlugin.addExecution(excuteAnalyse);
            if (!build.getPlugins().contains(dependencyPlugin)){
                build.addPlugin(dependencyPlugin);
                MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
                mavenXpp3Writer.write(new FileWriter(completePath),model);
            }
            fr.close();
            //执行mvn compile
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile( new File( completePath ) );
            request.setGoals(Arrays.asList("dependency:tree","dependency:analyze"));
            Invoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File("D:\\ProductionTools\\maven\\apache-maven-3.6.3-bin\\apache-maven-3.6.3"));
            invoker.setLogger(new PrintStreamLogger(System.err,  InvokerLogger.ERROR){
            } );
            invoker.setOutputHandler(new InvocationOutputHandler() {
            @Override
            public void consumeLine(String s) throws IOException {
                writer.write(s+"\n");
            }
            });
            try
            {
                System.out.println(invoker.execute( request ).getExitCode());
            }
            catch (MavenInvocationException e)
            {
                e.printStackTrace();
                return false;
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("IOException");
        }catch (XmlPullParserException e) {
            System.out.println("XmlPullParserException");
            return false;
        }
        return true;
    }
    public boolean handlebash(){
        return true;
    }
}