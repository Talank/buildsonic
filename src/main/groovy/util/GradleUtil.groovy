package util

class GradleUtil {
    enum GradleStrategy {
        GRADLE_PARALLEL_TEST, GRADLE_FORK_TEST, GRADLE_REPORT_GENERATION, GRADLE_COMPILER_DAEMON, GRADLE_INCREMENTAL_COMPILATION,
        PARALLEL_BUILDS, FILE_SYSTEM_WATCHING, CONFIGURATION_ON_DEMAND, CACHING, GRADLE_DAEMON
    }

    enum GradleCategory {
        PROPERTIES,
        COMPILATION,
        TEST,
        FORK
    }

    static Map<GradleCategory, List<GradleStrategy>> strategiesOfCategory =
            [(GradleCategory.PROPERTIES): [GradleStrategy.CACHING,
                                           GradleStrategy.FILE_SYSTEM_WATCHING,
                                           GradleStrategy.CONFIGURATION_ON_DEMAND,
                                           GradleStrategy.PARALLEL_BUILDS,
                                           GradleStrategy.GRADLE_DAEMON],
             (GradleCategory.COMPILATION): [GradleStrategy.GRADLE_COMPILER_DAEMON,
                                            GradleStrategy.GRADLE_INCREMENTAL_COMPILATION],
             (GradleCategory.TEST): [GradleStrategy.GRADLE_PARALLEL_TEST,
                                     GradleStrategy.GRADLE_REPORT_GENERATION],
             (GradleCategory.FORK):[GradleStrategy.GRADLE_FORK_TEST]]

    static GradleCategory getGradleCategory(GradleStrategy strategy){
        for(GradleCategory category : strategiesOfCategory.keySet()){
            def strategies = strategiesOfCategory.get(category)
            if (strategies.contains(strategy)){
                return category
            }
        }
        return null
    }
}