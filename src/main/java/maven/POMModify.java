package maven;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 传入需要修改的POM的绝对路径，然后根据ArrayList<String> targetDependency去修改,pomList是用来找对应父POM的
 * 读取pom.xml文件，首先查找<dependency></>,如果dep的name匹配，并且<version></>不为空并且不带${}符号，那么直接修改<version></>内容
 * 否则就去查看<properties></>,把原来的versionContent：${xxx}变成xxx,看能否在<properties>里面找到对应value
 * 如果找到，就修改<properties><xxxx>的对应内容
 * 否则，这个需要修改的依赖的版本号就是从父POM继承过来的，然后把这些需要到父POM解决的targetDependency放到实例域parentModify
 * 再通过之前传入的pomList找到对应的父POM，然后调用POMModify,传入父POM的这些参数
 * POMModify fpomModify = new POMModify().modify(fPom.getPath(),pomList,parentModify);
 */
public class POMModify {

    private Document doc;
    private XPath xpath;
    private ArrayList<String> parentModify = new ArrayList<>();

    public POMModify modify(String pomAbsolutePath, ArrayList<POM> pomList , ArrayList<String> targetDependency) throws Exception {
        String relativePath = targetDependency.get(1).substring(5);
        //String pomAbsolutePath = filePath + "/" + relativePath.replace(".","/") + "/pom.xml";
        String content = turnDocumentToString(pomAbsolutePath);

        if (!content.equals("")) {
            doc = getDocument(content);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            xpath = xPathfactory.newXPath();

            for(int i = 2; i < targetDependency.size(); i++){
                //字符串数组，有四个值，分别是dep的name、versionContent、version、modifiedVersion
                String[] targetDep = getStrings(targetDependency.get(i));
                if(!modifyDependencies(targetDep)){
                    if(!modifyProperties(targetDep)){
                        parentModify.add(targetDependency.get(i));
                    }
                }
            }

            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult sr = new StreamResult(pomAbsolutePath);
            tf.transform(source, sr);

            if(parentModify!=null){

                POM currentPom = null;
                for (POM pom:pomList){
                    if(pom.getRelativePath().equals(relativePath)){
                        currentPom = pom;
                        break;
                    }
                }
                if(currentPom!=null && currentPom.hasParent()){
                    POM fPom = currentPom.getParent();
                    parentModify.add(0," ");
                    parentModify.add(1,fPom.getRelativePath());
                    POMModify fpomModify = new POMModify().modify(fPom.getPath(),pomList,parentModify);
                }
            }
        }
        return null;
    }

    private String[] getStrings(String string){
        String[] targetDep = new String[4];

        //dependency: {Name: com.alibaba.cola/cola-component-dto, VersionContent: ${cola.components.version} , Version: 4.1.0-SNAPSHOT }  ------->4.0.1
        //com.alibaba.cola/cola-component-dto, VersionContent: ${cola.components.version} , Version: 4.1.0-SNAPSHOT }  ------->4.0.1
        String targetDepRaw = string.substring(19);

        //com.alibaba.cola/cola-component-dto
        String name = targetDepRaw.split(",")[0].trim();



        //VersionContent: ${cola.components.version}
        //${cola.components.version}
        String versionContent = targetDepRaw.split(",")[1].split(":")[1].trim();


        //Version: 4.1.0-SNAPSHOT }  ------->4.0.1
        String versions = targetDepRaw.split(",")[2];


        //4.1.0-SNAPSHOT
        String version = versions.split("}")[0].substring(9).trim();

        //4.0.1
        String modifiedVersion = string.split(">")[1].trim();

        targetDep[0]=name;
        targetDep[1]=versionContent;
        targetDep[2]=version;
        targetDep[3]=modifiedVersion;
        return targetDep;
    }

    private Document getDocument(String content) throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(content));
        return builder.parse(inputSource);
    }

    private String replaceProperties(String string) throws Exception {
        String patternString = "(.*?)\\$\\{(.+?)}([^$]*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(string);

        String replaced = "";

        while(matcher.find()) {
            String prefix = matcher.group(1);
            String propertyName = matcher.group(2);
            String suffix = matcher.group(3);
            replaced = replaced + prefix + propertyName + suffix;
        }
        if (!replaced.equals("")) {
            return replaced;
        }
        return string;
    }

    private boolean modifyProperties(String[] targetDep) throws Exception{
        for (Node node : getNodes("/project/properties/*")) {
            String name = node.getNodeName();
            String value = node.getTextContent();
            String versionContent = replaceProperties(targetDep[1]);    //去掉${ }
            if (name.equals(versionContent) && value.equals(targetDep[2])){
                node.setTextContent(targetDep[3]);
                System.out.println("Modify property: " + name + ": " + value + "---->" + targetDep[3]);
                return true;
            }
        }
        return false;
    }

    private boolean modifyDependencies(String[] targetDep) throws Exception{
       // System.out.println("targetDep: " + targetDep[0]);
        for (Node node : getNodes(
                "/project/dependencies/dependency|/project/dependencyManagement/dependencies/dependency")) {
            NodeList childNodes = node.getChildNodes();

            String groupID = "";
            String artificatID = "";
            String version = "";

            for (int i=0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (Node.ELEMENT_NODE == childNode.getNodeType()) {

                    String tag = childNode.getNodeName();

                    if (tag.equals("groupId")) {
                        groupID = childNode.getTextContent();
                        groupID = replaceProperties(groupID);
                    } else if (tag.equals("artifactId")) {
                        artificatID = childNode.getTextContent();
                        artificatID = replaceProperties(artificatID);
                    } else if (tag.equals("version")) {
                        version = childNode.getTextContent();
                      //  version = replaceProperties(version);
                    }
                }
            }


            String name = groupID + '/' + artificatID;
            name = name.trim();
          //  System.out.println("dep: " + name + "  version: " + version);

            if(name.equals(targetDep[0])){
             //   System.out.println("the same name,and it's version: "  + version);
                //找到pom.xml中的目标dep
                if(version.equals("") || version.startsWith("$")) {
                    return false;
                }else if(version.equals(targetDep[1])){
                    //直接修改
                    for (int i=0; i < childNodes.getLength(); i++) {
                        Node childNode = childNodes.item(i);
                        if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                            String tag = childNode.getNodeName();
                            if (tag.equals("version")) {
                                childNode.setTextContent(targetDep[3]);
                                System.out.println("Modify dependency<version>: " + targetDep[1] + "---->" +targetDep[3]);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private String turnDocumentToString(String path) {
        try {
            // 读取 xml 文件
            File fileinput = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileinput);

            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);


            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<Node> getNodes(String name) throws Exception {
        XPathExpression expr = xpath.compile(name);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        ArrayList<Node> result = new ArrayList<>();

        for (int i=0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                result.add(childNode);
            }
        }
        return result;
    }

    private String getValue(String name) throws Exception {
        XPathExpression expr = xpath.compile(name);
        return expr.evaluate(doc);
    }
}
