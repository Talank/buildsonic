package smell.fixer.Maven

import com.ximpleware.AutoPilot
import com.ximpleware.VTDGen
import com.ximpleware.VTDNav
import com.ximpleware.XMLModifier

class POMFixer {
    String filePath
    VTDGen vtdGen
    VTDNav vn
    XMLModifier xm
    AutoPilot ap
    List<MavenProperty> properties = new ArrayList<>()
    List<MavenDependency> dependencies = new ArrayList<>()
    List<MavenPlugin> plugins = new ArrayList<>()
    List<String> lines

    POMFixer(String filePath) {
        this.filePath = filePath
        this.vtdGen = new VTDGen()
        this.vtdGen.parseFile(filePath, true)
        this.vn = vtdGen.getNav()
        this.ap = new AutoPilot(this.vn)
        this.xm = new XMLModifier(this.vn);
        this.lines = new File(this.filePath).readLines()
    }

    public String getLine(String mark) {
        String line = null
        this.lines.each {
            if (it.contains(mark)) {
                line = it
                return
            }
        }
        return line
    }

    public String getSpaces(String mark) {
        String line = getLine(mark)
        if (line == null) {
            return "\t"
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < line.size(); i++) {
            char tmp = line.charAt(i)
            if(tmp.isWhitespace()) {
                sb.append(tmp)
            } else {
                break
            }
        }
        return sb.toString()
    }

    public List<MavenProperty> getProperties() {
        if (this.properties.size() > 0)
            return this.properties
        this.ap.selectXPath("/project/properties/*");
        while(ap.evalXPath() !=-1){
            String tag = vn.toString(vn.getCurrentIndex())
            String content = vn.toString(vn.getText())
            long l = vn.getElementFragment()
            MavenProperty mavenProperty = new MavenProperty(tag, content, (int) l, (int) (l >> 32))
            this.properties << mavenProperty
        }
        this.ap.resetXPath()
        //遍历完成后 似乎不需要手动重置cursor位置
        return this.properties
    }

    public List<MavenDependency> getDependencies() {
        if (this.dependencies.size() > 0)
            return this.dependencies
        this.ap.selectXPath("/project/dependencies/dependency|/project/dependencyManagement/dependencies/dependency");
        while(ap.evalXPath() !=-1){
            String groupId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "groupId")) {
                groupId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            String artifactId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "artifactId")) {
                artifactId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            long l = vn.getElementFragment()
            MavenDependency mavenDependency = new MavenDependency(groupId, artifactId, (int) l, (int) (l >> 32))

            this.dependencies << mavenDependency
        }
        this.ap.resetXPath()
        //遍历完成后 似乎不需要手动重置cursor位置
        return this.dependencies
    }

    public List<MavenPlugin> getPlugins() {
        if (this.plugins.size() > 0)
            return this.plugins
        this.ap.selectXPath("//plugin");
        while(ap.evalXPath() != -1){
            String groupId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "groupId")) {
                groupId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            String artifactId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "artifactId")) {
                artifactId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            String version = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "version")) {
                version = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }

            Map<String, String> configurationMap = new LinkedHashMap<>()
            if (vn.toElement(VTDNav.FIRST_CHILD, "configuration")) {
                if (vn.toElement(VTDNav.FIRST_CHILD)) {
                    iterateConfiguration(configurationMap)
                    vn.toElement(VTDNav.PARENT)
                }
                vn.toElement(VTDNav.PARENT)
            }
            MavenPlugin mavenPlugin = new MavenPlugin(groupId, artifactId, version, configurationMap)
            this.plugins << mavenPlugin
        }
        //遍历完成后 似乎不需要手动重置cursor位置
        this.ap.resetXPath()
        return this.plugins
    }

    //判断pom文件是否包含<properties>
    public boolean containsProperties() {
        this.ap.selectXPath("/project/properties");
        boolean flag = false
        if (ap.evalXPath() !=-1){
            flag = true
        }
        this.ap.resetXPath()
        //遍历完成后 似乎不需要手动重置cursor位置
        return flag
    }

    public boolean insertProperties(String spaces, String tag, String content) {
        this.ap.selectXPath("/project/description|/project/name|/project/version");
        if(ap.evalXPath() !=-1){
            xm.insertAfterElement("""
${spaces}<properties>
${spaces}\t<${tag}>${content}</${tag}>
${spaces}</properties>
""")
        }
        this.ap.resetXPath()
        //遍历完成后 似乎不需要手动重置cursor位置
        vn.toElement(VTDNav.ROOT)
        return true
    }
    //pom文件中插入<property>元素
    public boolean insertProperty(String tag, String content) {
        String spaces = getSpaces("<modelVersion>")
        if(!containsProperties()) {
            insertProperties(spaces, tag, content)
            return true
        }
        this.ap.selectXPath("/project/properties/*[last()]");
        if(ap.evalXPath() !=-1){
            xm.insertAfterElement("\n${spaces}\t<${tag}>${content}</${tag}>")
        }
        this.ap.resetXPath()
        //遍历完成后 似乎不需要手动重置cursor位置
        return true
    }

    public String getLatestReleaseVersion() {
        this.ap.selectXPath("/metadata/versioning/release")
        String version = null
        while (this.ap.evalXPath() != -1) {
            int t = vn.getText(); // get the index of the text (char data or CDATA)
            if (t!=-1)
                version = vn.toNormalizedString(t).trim()
        }
        this.ap.resetXPath()
        vn.toElement(VTDNav.ROOT)
        return version
    }
    //移除对应的Dependency
    public boolean removeDependency(String groupId, String artifactId) {
        Boolean flag = false
        MavenDependency md = null
        for (MavenDependency mavenDependency : getDependencies()) {
            if (mavenDependency.getGroupId().equals(groupId) && mavenDependency.getArtifactId().equals(artifactId)) {
                md = mavenDependency
                flag = true
            }
        }
        if (md != null)
            xm.removeContent(md.getOffset(), md.getLen())
        return flag
    }

    public void iterateConfiguration(Map<String, String> configurationMap) {
        do {
            String key = vn.toString(vn.getCurrentIndex())
            String value = null
            int t = vn.getText()
            if (t == -1) {
                long l = vn.getContentFragment();
                if (l != -1)
                    value = vn.toString((int) l, (int) (l >> 32))
            } else {
                value = vn.toNormalizedString(vn.getText())
            }
            configurationMap.put(key, value)
        } while (vn.toElement(VTDNav.NEXT_SIBLING))
    }


    public MavenPlugin getSurefirePlugin() {
        for (MavenPlugin mavenPlugin : getPlugins()) {
            if (mavenPlugin.getArtifactId().equals("maven-surefire-plugin")) {
                return mavenPlugin
            }
        }
        return null
    }

    public MavenPlugin getCompilerPlugin() {
        for (MavenPlugin mavenPlugin : getPlugins()) {
            if (mavenPlugin.getArtifactId().equals("maven-compiler-plugin")) {
                return mavenPlugin
            }
        }
        return null
    }

    public boolean updatePluginNodeConfiguration(String pluginName, String tag, String value) {
        // pluginName = 'maven-compiler-plugin' or 'maven-surefire-plugin'
        MavenPlugin mavenPlugin = pluginName.contains('compiler')?getCompilerPlugin():getSurefirePlugin()
        if (mavenPlugin == null)
            return false
        this.ap.selectXPath("//plugin");
        while(ap.evalXPath() != -1){
            String artifactId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "artifactId")) {
                artifactId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            if (artifactId==pluginName) {
                if (vn.toElement(VTDNav.FIRST_CHILD, "configuration")) {
                    if(vn.toElement(VTDNav.FIRST_CHILD, tag)) {
                        xm.updateToken(vn.getText(), value)
                        vn.toElement(VTDNav.PARENT)
                    }
                    vn.toElement(VTDNav.PARENT)
                }
            }
        }
        this.ap.resetXPath()
        return true
    }

    boolean insertConfiguration(String space, Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        if (vn.toElement(VTDNav.FIRST_CHILD, "configuration")) {
            vn.toElement(VTDNav.LAST_CHILD)
            map.each {String tag, String content ->
                sb.append("\n${space}\t<${tag}>${content}</${tag}>")
            }
            //xm.insertAfterElement("\n${space}\t<parallel>classes</parallel>\n${space}\t<useUnlimitedThreads>true</useUnlimitedThreads>\n")
            xm.insertAfterElement(sb.toString())
            vn.toElement(VTDNav.PARENT)
            vn.toElement(VTDNav.PARENT)
        } else {
            vn.toElement(VTDNav.LAST_CHILD)
            sb.append("\n${space}<configuration>\n")
            map.each {String tag, String content ->
                sb.append("${space}\t<${tag}>${content}</${tag}>\n")
            }
            sb.append("${space}</configuration>")
            //xm.insertAfterElement("\n${space}<configuration>\n${space}\t<parallel>classes</parallel>\n${space}\t<useUnlimitedThreads>true</useUnlimitedThreads>\n${space}</configuration>\n")
            xm.insertAfterElement(sb.toString())
            vn.toElement(VTDNav.PARENT)
        }
        return true
    }
    
    Map<Boolean,String> getPluginNodeSpace(String pluginName){
        // 如果同时存在//build/plugins/plugin与//build/pluginManagement/plugins/plugin
        //只修改//build/plugins/plugin
        Map<Boolean,String> spaceMap = new HashMap<>()
        String pluginLine = null
        int pluginIndex = -1
        Boolean isPluginManagement = false
        int[] pluginsIndex = new int[]{-1,-1}
        for(int i = 1 ; i < this.lines.size() ; i++){
            if(lines[i].trim()=='<plugins>'){
                int index = i-1
                while(index>=0 && lines[index].trim()==''){
                    index--
                }
                if(lines[index].trim()=='<pluginManagement>'){
                    //  build/pluginManagement/plugins/plugin
                    pluginsIndex[0] = i
                }else{
                    pluginsIndex[1] = i
                }
//                else if(lines[index].trim()=='<build>'){
//                    //  build/plugins/plugin
//                    pluginsIndex[1] = i
//                }
            }
            if(pluginsIndex[1]!=-1){
                break
            }
        }

        //再找到具体的plugin
        //先在build/plugins/plugin里面找，如果找到了直接return
        if(pluginsIndex[1]!=-1){
            pluginIndex = pluginsIndex[1]
            while( pluginIndex < this.lines.size() ){
                if(lines[pluginIndex].trim()=="<artifactId>${pluginName}</artifactId>"){
                    pluginLine = lines[pluginIndex]
                    break
                }
                pluginIndex++
            }
        }
        // 如果在build/plugins/plugin里面没找到，那么就去build/pluginManagement/plugins/plugin里面找
        if(pluginIndex==-1 || pluginIndex==this.lines.size()){
            if(pluginsIndex[0]==-1){
                return null
            }
            pluginIndex = pluginsIndex[0]
            while( pluginIndex < this.lines.size() ){
                if(lines[pluginIndex].trim()=="<artifactId>${pluginName}</artifactId>"){
                    pluginLine = lines[pluginIndex]
                    break
                }
                pluginIndex++
            }
            isPluginManagement = true
        }

        //提前判断过pom文件是否存在该plugin,所以这个if理论上不会执行
        if(pluginLine==null){
            println("没有找到${pluginName}")
            return null
        }

        //抽取space
        StringBuilder sb = new StringBuilder()
        for(int i = 0; i < pluginLine.size(); i++) {
            char tmp = pluginLine.charAt(i)
            if(tmp.isWhitespace()) {
                sb.append(tmp)
            } else {
                break
            }
        }
        spaceMap.put(isPluginManagement,sb.toString())
        return spaceMap
    }

    public boolean editCompilerNode(Map<String, String> map) {
        MavenPlugin mavenPlugin = getCompilerPlugin()
        if (mavenPlugin == null)
            return false
//        String space = getSpaces("maven-compiler-plugin")
        def spaceMap = getPluginNodeSpace("maven-compiler-plugin")
        boolean isPluginManagement = null
        String space = null
        spaceMap.each {
            isPluginManagement = it.key
            space = it.value
        }
        if(isPluginManagement){
            this.ap.selectXPath("//build/pluginManagement/plugins/plugin")
        }else{
            this.ap.selectXPath("//build/plugins/plugin")
        }
        while(ap.evalXPath() != -1){
            String artifactId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "artifactId")) {
                artifactId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            if (artifactId.equals('maven-compiler-plugin')) {
                insertConfiguration(space, map)
            }
        }
        this.ap.resetXPath()
        return true
    }

    public boolean editSurefireNode(Map<String, String> map) {
        MavenPlugin mavenPlugin = getSurefirePlugin()
        if (mavenPlugin == null)
            return false
//        String space = getSpaces("maven-surefire-plugin")
        def spaceMap = getPluginNodeSpace("maven-surefire-plugin")
        boolean isPluginManagement = null
        String space = null
        spaceMap.each {
            isPluginManagement = it.key
            space = it.value
        }
        if(isPluginManagement){
            this.ap.selectXPath("//build/pluginManagement/plugins/plugin")
        }else{
            this.ap.selectXPath("//build/plugins/plugin")
        }
        while(ap.evalXPath() != -1){
            String artifactId = null
            if (vn.toElement(VTDNav.FIRST_CHILD, "artifactId")) {
                artifactId = vn.toNormalizedString(vn.getText())
                vn.toElement(VTDNav.PARENT)
            }
            if (artifactId.equals('maven-surefire-plugin')) {
                insertConfiguration(space, map)
            }
        }
        this.ap.resetXPath()
        return true
    }

    public void printToFile(String filePath) {
        this.xm.output(filePath)
    }
}
