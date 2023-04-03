package smell.fixer.Gradle


import java.nio.file.Paths

import static util.GradleUtil.GradleCategory
import static util.GradleUtil.GradleStrategy

/**
 * 传入项目路径repoPath，读取项目路径下的build.gradle文件，读至ArrayList<String> content
 * 如果原文件存在allprojects{}，allProjectIndex记录allproject{出现的位置，如果没有就设置为-1
 * 如果allProjectIndex ！= -1，就在这个位置的下面插入新内容
 * 否则在文件末尾，插入新内容，并且用allproject{}包起来
 * 默认在原build.gradle文件基础上做修改，如果要在不改变原来文件的基础上，生成新的，修改writeFile()里的路径
 */
class BuildGradleFixer {
    private List<String> content = new ArrayList<>();
    private String filePath;
    private int allProjectIndex = -1;

    BuildGradleFixer(String repoPath) {
        this.filePath = Paths.get(repoPath,"build.gradle").normalize().toString();
        File file = new File(this.filePath);
        if(!file.exists()) {
            //抛异常还是创建新文件？
            throw new IOException("${this.filePath} 文件不存在")
        }
        this.content = new File(this.filePath).readLines();
        content.eachWithIndex{ String line, int index ->
            if(line.startsWith("allproject")){
                this.allProjectIndex = index;
            }
        }
    }

    void modifyGradle(GradleCategory category, List<GradleStrategy> strategies){
        System.out.println("开始修改build.gradle文件，地址为：" + this.filePath);
        String addContent = "";
        if(category == GradleCategory.TEST){
            addContent = getAddContentOfTest(strategies);
        } else if(category == GradleCategory.COMPILATION){
            addContent = getAddContentOfCompilation(strategies);
        } else if(category == GradleCategory.FORK){
            addContent = getAddContentOfFork(strategies)
        }
        if(this.allProjectIndex == -1){   //如果没有allprojects{}，就插入到末尾
            content.add("\nallprojects {");
            content.add(addContent);
            content.add("}\n");
        }else{
            content.add(allProjectIndex + 1, addContent);
        }
        new File(this.filePath).text = content.join('\n')   //写修改后的文件
    }

    static String getAddContentOfCompilation(List<GradleStrategy> strategies){
        List<String> addContent = new ArrayList<>()
        addContent.add("    tasks.withType(JavaCompile).configureEach {");
        String compilerDaemon = "        options.fork = true";
        String incrementalCompilation  = "        options.incremental = true"
        if (strategies.contains(GradleStrategy.GRADLE_COMPILER_DAEMON)) {
            addContent.add(compilerDaemon)
        }
        if (strategies.contains(GradleStrategy.GRADLE_INCREMENTAL_COMPILATION)) {
            addContent.add(incrementalCompilation)
        }
        addContent.add("    }\n");
        return addContent.join('\n')
    }

    static String getAddContentOfTest(List<GradleStrategy> strategies){
        List<String> addContent = new ArrayList<>()
        addContent.add("    tasks.withType(Test).configureEach {");
        String parallelTest = "        maxParallelForks = Runtime.runtime.availableProcessors().intdiv(2) ?: 1"
        String processForkingOptions = "        forkEvery = 100";
        String disableReportGeneration="        if (!project.hasProperty(\"createReports\")) {\n" +
                "            reports.html.required = false\n" +
                "            reports.junitXml.required = false\n" +
                "        }";
        if(strategies.contains(GradleStrategy.GRADLE_PARALLEL_TEST)) {
            addContent.add(parallelTest)
        }
        if(strategies.contains(GradleStrategy.GRADLE_FORK_TEST)) {
            addContent.add(processForkingOptions)
        }
        if(strategies.contains(GradleStrategy.GRADLE_REPORT_GENERATION)) {
            addContent.add(disableReportGeneration)
        }
        addContent.add("    }\n");
        return addContent.join('\n');
    }

    static String getAddContentOfFork(List<GradleStrategy> strategies){
        List<String> addContent = new ArrayList<>()
        addContent.add("    tasks.withType(Test).configureEach {")
        String processForkingOptions = "        forkEvery = 100"
        if(strategies.contains(GradleStrategy.GRADLE_FORK_TEST)) {
            addContent.add(processForkingOptions)
        }
        addContent.add("    }\n")
        return addContent.join('\n')
    }

    static void main(String[] args) {
    }
}