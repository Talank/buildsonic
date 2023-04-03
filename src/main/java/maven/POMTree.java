package maven;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class POMTree {
    @Getter
    private ArrayList<POM> pomList = new ArrayList<>();
    private Map<String, POM> pomMap = new HashMap<>();

    private String repoDir;
    private String repoName;

    private POM rootPom = null;

    public POMTree(String repoDir) {
        this.repoDir = repoDir;
        this.repoName = Paths.get(repoDir).getFileName().toString();
    }

    public POM createPomTree() throws Exception {
        if (this.rootPom != null)
            return this.rootPom;

        String rootPomFilePath = Paths.get(this.repoDir, "pom.xml").normalize().toString();
        File file = new File(rootPomFilePath);
        if (!file.exists()) {
//            System.out.println("根目录pom文件" + rootPomFilePath + "不存在");
            return null;
        }
//        if (!file.exists()) {
//           throw new Exception("根目录pom文件" + rootPomFilePath + "不存在");
//        }
        //生成聚合树
        this.rootPom = createAggregatorTree(rootPomFilePath);
        this.pomList.addAll(this.pomMap.values());
        //生成继承树关系
        createInheritanceTree();

        //setRelativePath(projectName);
        return rootPom;
    }

    public POM createAggregatorTree(String pomFilePath) throws Exception {
        if(this.pomMap.get(pomFilePath) != null) {
            return this.pomMap.get(pomFilePath);
        }
        POMParser pomParser = new POMParser();
        POM rootPom = pomParser.parse(pomFilePath);
        this.pomMap.put(pomFilePath, rootPom);
        if (rootPom.getPackaging().equals("pom")) {
            //深度优先遍历
            for (String moduleName : rootPom.getModules()) {
                String modulePath = Paths.get(Paths.get(pomFilePath).getParent().toString(), moduleName, "pom.xml").normalize().toString();
                File file = new File(modulePath);
                if(!file.exists()){
//                    System.out.println("Module: pom文件" + modulePath + "不存在");
                    continue;
                }
                //System.out.println(modulePath);
                POM modulePom = createAggregatorTree(modulePath);
                rootPom.addAggregatorPom(modulePom);
            }
        }
        return rootPom;
    }

    public void createInheritanceTree() {
        //对于每个pom，找到其父POM
        for (POM childPom : this.pomList) {
            //如果该POM有父POM
            if (childPom.hasParent()) {
                //去PomList里面找到这个父POM，并覆盖之前的Parent POM变量
                for (POM parentPom : this.pomList) {
                    if (parentPom.getGroupId().equals(childPom.getParentGroupId()) && parentPom.getArtifactId().equals(childPom.getParentArtifactId())) {
                        parentPom.addChildPom(childPom);
                    }
                }
            }
        }
    }


    public List<Dependency> getRawDependencies() {
        List<Dependency> list = new ArrayList<>();
        for (POM pom : pomList) {
            List<Dependency> dependencies = pom.getRawDependencies();
            for (Dependency dependency : dependencies) {
                if (!dependency.getVersion().equals("")) {
                    list.add(dependency);
                }
            }
        }
        return list;
    }

    public List<Dependency> getDependencies() {
        List<Dependency> list = new ArrayList<>();
        for (POM pom : pomList) {
            List<Dependency> dependencies = pom.getDependencies();
            list.addAll(dependencies);
        }
        return list;
    }

    public List<Dependency> getDynamicDependencies() {
        List<Dependency> dependencies = getDependencies();
        List<Dependency> list = new ArrayList<>();
        List<String> artifactIds = getArtifactIds();
        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion();
            if (version.equals("RELEASE") ||version.equals("LATEST") || version.endsWith("SNAPSHOT")) {
                if(!artifactIds.contains(dependency.getArtifactID()))
                    list.add(dependency);
            }
        }
        return list;
    }

    public Map<String, List<Dependency>> getDependenciesMap() {
        Map<String, List<Dependency>> map = new LinkedHashMap<>();
        for (POM pom : this.pomList) {
            map.put(pom.getPath(), pom.getDependencies());
        }
        return map;
    }

    public List<String> getPathList() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.add(pom.getPath());
        }
        return list;
    }

    public List<String> getNames() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.add(pom.getName());
        }
        return list;
    }
    public List<String> getArtifactIds() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.add(pom.getArtifactId());
        }
        return list;
    }

    public List<String> getRepositoryUrls() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.addAll(pom.getRepositoryUrls());
        }
        return list.stream().distinct().collect(Collectors.toList());
    }
}
