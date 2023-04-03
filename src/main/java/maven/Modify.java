package maven;
import java.io.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * filePath：项目仓库
 * targetDependencies方法里面的path：通过python爬取下来的信息
 */
public class Modify {
    public static void main(String[] args) throws Exception {
        String filePath = "F:/Maven01/TestProject/ModifyTest";
        File file = new File(filePath); //需要获取的文件的路径
        File[] fileList = file.listFiles(); //存储文件路径的String数组
        for (File f:fileList){
            if (f.isDirectory()) {
                String filename = f.getName();
                String path = f.getAbsolutePath();
                POMTree pomTree = new POMTree(path);
                pomTree.createPomTree();
                ArrayList<POM> pomList = pomTree.getPomList();
                if (pomList == null) {
                    continue;
                }

                List<String> pathList = pomTree.getPathList();
                HashSet<ParentLinkList.Node> parentLinkLists = new ParentLinkList().getParentLinkLists(pomList);


                ArrayList<ArrayList<String>> targetDependencies = null;
                try {
                    targetDependencies = targetDependencies(filename);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (targetDependencies == null){
                    continue;
                }

                for (ArrayList<String> targetDependency : targetDependencies) {
                    String pomRelativePath = targetDependency.get(1).substring(5);
                    String pomAbsolutePath = filePath + "/" + pomRelativePath.replace(".","/") + "/pom.xml";
                    POMModify pomModify = new POMModify();
                    try {
                        pomModify.modify(pomAbsolutePath, pomList,targetDependency);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static ArrayList<ArrayList<String>> targetDependencies(String filename) throws IOException {
        String path = "F:/Maven01/TestProject/pythonTestC/modify_alibaba@COLA.txt";
        FileInputStream inputStream = new FileInputStream(path);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        ArrayList<ArrayList<String>> targetDependencies = new ArrayList<>();
        String str = null;
        while((str = bufferedReader.readLine()) != null)
        {
            if(str.startsWith("POM")){
                ArrayList<String> targetDependency = new ArrayList<>();
                targetDependency.add(str);
                while((str = bufferedReader.readLine())!= null &&!str.equals("")){
                    targetDependency.add(str);
                }
                targetDependencies.add(targetDependency);
            }
        }
        //close
        inputStream.close();
        bufferedReader.close();
        return targetDependencies;
    }
}
