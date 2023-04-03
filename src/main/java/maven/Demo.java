package maven;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Demo {
    public static void main(String[] args) throws Exception {
        String filePath = "/home/fdse/user/zc/sequence/repository";
        File file = new File(filePath); //需要获取的文件的路径
        File[] fileList = file.listFiles(); //存储文件路径的String数组
        for (File f:fileList){
            if (f.isDirectory()) {
                StringBuffer content = new StringBuffer("");
                String filename = f.getName();
                String path = f.getAbsolutePath();
                POMTree pomTree = new POMTree(path);
                pomTree.createPomTree();
                ArrayList<POM> pomList = pomTree.getPomList();

                if(pomList==null){
                    continue;
                }

                for(POM pom:pomList){

                    content.append(pom.toString()).append("\n");
                    content.append("POM路径：").append(pom.getRelativePath()).append("\n");
                    ArrayList<Dependency> dependencies;
                    dependencies = pom.getDependencies();

                    content.append("该POM所有的依赖：").append("\n");
                    for(Dependency dep: dependencies) {
                        //保存所有的依赖信息
                        content.append(dep.toString()).append("\n");
                    }

                    content.append("该POM的不稳定依赖：").append("\n");
                    for(Dependency dep: dependencies) {
                        VersionSpecifier versionSpecifier = dep.getVersionSpecifier();
                        if (isTargetVersion(versionSpecifier)) {
                            content.append(dep.toString()).append("\n");
                        }
                    }

                    content.append("\n--------------------------------------------------------\n\n");
//                    System.out.println(pom.getRelativePath() +"   "+ pom.toString());
//                    if (pom.hasParent()){
//                        System.out.println("父POM名字：" + pom.getParent().toString());
//                        System.out.println("父POM地址：" + pom.getParent().getRelativePath());
//                    }
//                    System.out.println("--------------------------------------------------------");

//                    输出test并行配置内容
                    HashMap<String,String> testConfigurations;
                    testConfigurations = pom.getTestConfigurations();
                    content.append(testContent(testConfigurations)).append("\n");
                    content.append("------------------------------------------------------------------------------------\n");
                }
                String str = new String(content);
                try {
                    saveResult(str,filename);
                } catch (IOException e) {
                    System.out.println("保存项目数据时出错：" + filename);
                    e.printStackTrace();
                }
                System.out.println("项目解析完毕：" + filename + "\n\n");
            }
        }
    }

    public static void saveResult(String content, String fileName) throws IOException {
        String dir ="/home/fdse/user/zc/MavenDependencyParser/result" + "/"+fileName +".txt";
        File file = new File(dir);
        //如果文件不存在，创建文件
        if (!file.exists()) {
            file.createNewFile();
        }
        //创建FileWriter对象
        FileWriter writer = new FileWriter(file);
        //向文件中写入内容
        writer.write(content);
        writer.flush();
        writer.close();
    }

    public static String testContent(HashMap<String,String> testConfigurations){
        String[] parameters = {"parallel","useUnlimitedThreads","threadCount","perCoreThreadCount",
                "threadCountSuites","threadCountClasses","threadCountMethods","forkCount","reuseForks","argLine","systemPropertyVariables"};
        StringBuffer content = new StringBuffer("");
        content.append("\n*******************test配置信息*****************************\n");
        if(!testConfigurations.isEmpty()){
            Iterator<Map.Entry<String, String>> entries = testConfigurations.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();
                content.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            content.append("\n*******************并行配置信息*****************************\n");
            for (String parameter:parameters){
                if(testConfigurations.get(parameter)!=null){
                    content.append(parameter).append(": ").append(testConfigurations.get(parameter)).append("\n");
                }
            }
        }
        return content.toString();
    }

    public static boolean isTargetVersion(VersionSpecifier versionSpecifier){
        String version = versionSpecifier.getRaw();

        if(version.matches("^.*(SNAPSHOT).*$")){
            return true;
        }

        if(version.equals("RELEASE") ||version.equals("LATEST")){
            return true;
        }
//        if(versionSpecifierString.matches("^.*(RELEASE|SNAPSHOT|LATEST).*$")){
//            return true;
//        }
        return false;
    }
}