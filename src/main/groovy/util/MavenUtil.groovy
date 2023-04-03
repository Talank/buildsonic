package util

class MavenUtil {
    static enum MavenStrategy {
        MAVEN_PARALLEL_EXECUTION,
//        MAVEN_DYNAMIC_VERSION,//暂时不做这个
//        MAVEN_UNUSED_DEPENDENCY,

        MAVEN_PARALLEL_TEST,
        MAVEN_FORK_TEST,     //从TEST里面单独提出来
        MAVEN_REPORT_GENERATION,

        MAVEN_COMPILER_DAEMON,
//        MAVEN_INCREMENTAL_COMPILATION  //检测结果全为隐式OPEN
    }
    //git clone git@github.com:i-Taozi/MAVEN_PARALLEL_EXECUTION_Trigger.git

    static enum MavenCategory {
        PROPERTIES,
        COMPILATION,
        TEST,
        FORK
    }

    static Map<MavenCategory, List<MavenStrategy>> strategiesOfCategory =
            [(MavenCategory.PROPERTIES)       : [MavenStrategy.MAVEN_PARALLEL_EXECUTION],
             (MavenCategory.COMPILATION)      : [MavenStrategy.MAVEN_COMPILER_DAEMON],
             (MavenCategory.TEST)             : [MavenStrategy.MAVEN_PARALLEL_TEST,
                                                 MavenStrategy.MAVEN_REPORT_GENERATION],
             (MavenCategory.FORK)             :[ MavenStrategy.MAVEN_FORK_TEST]]

    static MavenCategory getMavenCategory(MavenStrategy strategy){
        for(MavenCategory category : strategiesOfCategory.keySet()){
            def strategies = strategiesOfCategory.get(category)
            if (strategies.contains(strategy)){
                return category
            }
        }
        return null
    }
}