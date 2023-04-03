package model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@RequiredArgsConstructor
@Entity
@Table(name = "repositories")
public class Repository {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //项目名
    @Column(name = "repo_name")
    private String repoName;

    //是否忽略这个项目 true表示忽略，false表示不忽略
    @Column(name = "ignore_repo")
    private Boolean ignoreRepo;

    //项目Star数量
    @Column(name = "star_number")
    private Integer starNumber;

    @Column(name = "last_build_number")
    private Integer lastBuildNumber;

    //项目最后构建时间，格式: 20xx-xx-xx xx:xx:xx
    @Column(name = "last_build_started_at")
    private Date lastBuildStartedAt;

    //是否存在 ".travis.yml"文件，true表示存在
    @Column(name = "contain_travis_yml")
    private Boolean containTravisYml;

    //构建工具，1表示Maven,2表示Gradle
    @Column(name = "build_tool")
    private Integer buildTool;

    //记录push构建 最近的开始时间
    @Column(name = "last_build_time")
    private Date lastBuildTime;

    //存储git depth的值 因为可能是false 数值，所以存为字符串类型
    @Column(name = "travis_git_depth")
    private String travisGitDepth;

    @Column(name = "travis_retry")
    private Boolean travisRetry;

    //存储travis_wait的时间
    @Convert(converter = IntegerListConverter.class)
    @Column(name = "travis_wait")
    private List<Integer> travisWait;

    @Column(name = "travis_cache")
    private Boolean travisCache;

    @Column(name = "travis_allow_failures")
    private Boolean travisAllowFailures;

    @Column(name = "travis_fast_finish")
    private Boolean travisFastFinish;

    //是否为多模块项目，true表示该项目为多模块项目
    @Column(name = "multi_module")
    private Boolean multiModule;

    //Gradle的parallel execution配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    //maven检测-T参数，不设置为默认值2，设置为-T 1为关闭(未发现有项目这样配置)，其他值则为OPEN
    @Column(name = "parallel_execution")
    private Integer parallelExecution;

    //是否为动态版本
    @Column(name = "dynamic_version")
    private Boolean dynamicVersion;

    @Column(name = "build_success")
    private Boolean buildSuccess;

    //是否存在未利用到的依赖
    @Column(name = "contain_unused_dependency")
    private Boolean containUnusedDependency;

    //Gradle的parallel test策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "parallel_test")
    private Integer parallelTest;

    /*项目配置的gradle version, x.x.x或者x.x格式，没有配置或没有检测的为null
       maven的版本也抽取一下，所以修改字段名
   */
    @Column(name = "version")
    private String version;

    //Gradle的file system watch策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "file_system_watch")
    private Integer fileSystemWatch;

    //Gradle的configure on demand策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = " configure_on_demand")
    private Integer configureOnDemand;

    //Gradle的gradle cache策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_cache")
    private Integer gradleCache;

    //Gradle的gradle daemon策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_daemon")
    private Integer gradleDaemon;

    //Gradle的gradle fork test策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_fork_test")
    private Integer gradleForkTest;

    //Gradle的gradle report generation策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_report_generation")
    private Integer gradleReportGeneration;

    //Gradle的gradle compiler daemon策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_compiler_daemon")
    private Integer gradleCompilerDaemon;

    //Gradle的gradle incremental compilation策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "gradle_incremental_compilation")
    private Integer gradleIncrementalCompilation;

    //项目中Java、Groovy、Kotlin、Scala、Clojure程序文件数量
    @Column(name = "java_files_num")
    private Integer javaFilesNum;

    //MAVEN的MAVEN_FORK_TEST策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "maven_fork_test")
    private Integer mavenForkTest;

    //MAVEN的MAVEN_REPORT_GENERATION策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "maven_report_generation")
    private Integer mavenReportGeneration;

    //MAVEN的MAVEN_COMPILER_DAEMON策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "maven_compiler_daemon")
    private Integer mavenCompilerDaemon;

    //MAVEN的MAVEN_INCREMENTAL_COMPILATION策略配置，如果开启了为1，关闭为0，默认为2，没有配置或者没有检测的为null
    @Column(name = "maven_incremental_compilation")
    private Integer mavenIncrementalCompilation;

    //是否包含测试文件
    @Column(name = "contain_test")
    private Integer containTest;
}
