package smell.checker

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import util.GradleUtil
import util.MavenUtil
import util.MysqlUtil
import util.SessionUtil
import util.TravisUtil

import static util.TravisUtil.TravisStrategy
import static util.GradleUtil.GradleStrategy
import static util.MavenUtil.MavenStrategy

class DataBaseChecker {


    static def run() {
        try (Session session = SessionUtil.getSession()) {
            def strategies = getStrategies()
            strategies.each {strategy->
                String buildTool = null
                if(strategy instanceof TravisStrategy){
                    buildTool = "(build_tool=1 or build_tool=2)"
                }else if(strategy instanceof MavenStrategy){
                    buildTool = "build_tool=1"
                }else{
                    buildTool = "build_tool=2"
                }
                def queryList = queryMap.get(strategy)
                if(queryList!=null){
                    println(strategy.toString())
                    queryList.each {queryKey->
                        if(queryKey!=""){
                            def query = session.createQuery("select count(*) from performance.repositories where ${buildTool} and ${queryKey};")
                            println(query.toString())
                        }else{
                            println("")
                        }
                    }
                }
            }
        }
    }

    static List<Object> getStrategies(){
        def list = []
        for(TravisStrategy strategy:TravisStrategy.values()){
            list << strategy
        }
        for(MavenStrategy strategy:MavenStrategy.values()){
            list << strategy
        }
        for(GradleStrategy strategy:GradleStrategy.values()){
            list << strategy
        }
        return list
    }

    // strategy对应List<String>[显式引入、隐式引入、显式消除、隐式消除]
    static Map<Object,List<String>> queryMap = [
            (TravisStrategy.TRAVIS_CACHE)                  :["travis_cache=0", "travis_cache is null", "travis_cache=1", ""],
            (TravisStrategy.TRAVIS_FAST_FINISH)            :["(travis_fast_finish is null or travis_fast_finish = false) and travis_allow_failures = true", "",
                                                             " travis_fast_finish= true and travis_allow_failures = true", ""],
            (TravisStrategy.TRAVIS_RETRY)                  :["travis_retry=1", "", "", "travis_retry=0 or travis_retry is null"],
            (TravisStrategy.TRAVIS_SHALLOW_CLONE)          :["travis_git_depth>50 or travis_git_depth =false", "", "(travis_git_depth<50 or travis_git_depth is null)", ""],
            (TravisStrategy.TRAVIS_WAIT)                   :["travis_wait is not null", "", "", "travis_wait is null"],
            //
            (GradleStrategy.CACHING)                       :["gradle_cache=0", "gradle_cache=2", "gradle_cache=1", ""],
            (GradleStrategy.CONFIGURATION_ON_DEMAND)       :["configure_on_demand=0", "configure_on_demand=2 and multi_module=1", "configure_on_demand=1", "configure_on_demand=2 and multi_module=0"],
            (GradleStrategy.FILE_SYSTEM_WATCHING)          :["file_system_watch=0", "file_system_watch=2 and version regexp'^6.[5-9]'",
                                                             "file_system_watch=1", "file_system_watch=2 and not (version regexp'^6.[5-9]')"],
            (GradleStrategy.PARALLEL_BUILDS)               :["parallel_execution=0", "parallel_execution=2 and multi_module=1",
                                                             "parallel_execution=1", "parallel_execution=2 and multi_module=0"],
            (GradleStrategy.GRADLE_DAEMON)                 :["gradle_daemon=0", "gradle_daemon=2 and version regexp'^[1-2].'",
                                                             "gradle_daemon=1", "gradle_daemon=2 and not (version regexp'^[1-2].'"],
            (GradleStrategy.GRADLE_COMPILER_DAEMON)        :["gradle_compiler_daemon=0", "gradle_compiler_daemon=2 and java_files_num>=1000",
                                                             "gradle_compiler_daemon=1", "gradle_compiler_daemon=2 and java_files_num<1000"],
            (GradleStrategy.GRADLE_INCREMENTAL_COMPILATION):["gradle_incremental_compilation=0", "gradle_incremental_compilation=2 and version regexp '^[1-4]' and not(version regexp '^4.10')",
                                                             "gradle_incremental_compilation=1", "gradle_incremental_compilation=2 and (version regexp '^[5-9]' or version regexp '^4.10' or version is null)"],
            (GradleStrategy.GRADLE_PARALLEL_TEST)          :["parallel_test=0", "parallel_test=2 ", "parallel_test=1", ""],
            (GradleStrategy.GRADLE_REPORT_GENERATION)      :["gradle_report_generation=0", "gradle_report_generation=2", "gradle_report_generation=1", ""],
            (GradleStrategy.GRADLE_FORK_TEST)              :["gradle_fork_test=0", "gradle_fork_test=2", "gradle_fork_test=1", ""],
            //
            (MavenStrategy.MAVEN_PARALLEL_EXECUTION)       :["parallel_execution=0", "parallel_execution=2 and multi_module=1",
                                                             "parallel_execution=1", "parallel_execution=2 and multi_module=0"],
            (MavenStrategy.MAVEN_COMPILER_DAEMON)          :["maven_compiler_daemon=0", "maven_compiler_daemon=2", "maven_compiler_daemon=1", ""],
            (MavenStrategy.MAVEN_PARALLEL_TEST)            :["parallel_test=0", "parallel_test=2", "parallel_test=1", ""],
            (MavenStrategy.MAVEN_REPORT_GENERATION)        :["maven_report_generation=0", "maven_report_generation=2", "maven_report_generation=1", ""],
            (MavenStrategy.MAVEN_FORK_TEST)                :[" maven_fork_test=0", " maven_fork_test=2", " maven_fork_test=1", ""]
    ]

}
