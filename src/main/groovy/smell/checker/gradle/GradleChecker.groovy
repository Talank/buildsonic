package smell.checker.gradle

import model.Repository
import smell.StateFlag
import smell.checker.gradle.BuildGradleChecker
import smell.checker.gradle.GradleOptionChecker
import util.GradleUtil

import static util.GradleUtil.*

class GradleChecker {

    /*
    /home/fdse/user/zc/sequence/repository/MirakelX@mirakel-android/new_ui/build.gradle
    /home/fdse/user/zc/sequence/repository/iwarapter@sonar-puppet/build.gradle
     */
    static StateFlag checkGradleProperties(Repository repository, String repoPath,GradleStrategy strategy) {
//        if (strategy == GradleStrategy.PARALLEL_BUILDS && !repository.multiModule) {
//            // 非多模块项目不应用该策略
//            return null
//        } else if(strategy == GradleStrategy.FILE_SYSTEM_WATCHING && compareVersion(repository.version, "7.0") >= 0) {
//            return null
//        }
        // 隐式引入需要考虑以下前置条件：后续再更改代码结构，先跑完显式引入修复
//        else if (strategy == GradleStrategy.CONFIGURATION_ON_DEMAND && !repository.multiModule) {
//            return null
//        } else if (strategy == GradleStrategy.GRADLE_DAEMON && compareVersion(repository.version, "3.0") >= 0) {
//            return null
//        }

        return GradleOptionChecker.gradleChecker(repoPath, repository.repoName, strategy)
    }

    static StateFlag checkBuildGradle(Repository repository, String repoPath, GradleStrategy strategy) {
        //首先检测前置条件
        if (strategy == GradleStrategy.GRADLE_COMPILER_DAEMON && repository.getJavaFilesNum() < 1000) {
            //超过1000个java文件才配置
            return null
        } else if(strategy == GradleStrategy.GRADLE_INCREMENTAL_COMPILATION && compareVersion(repository.getVersion(), "4.10") >= 0) {
            //gradle版本大于4.10的默认开启该功能
            return null
        }
        return BuildGradleChecker.check(repoPath, strategy)
    }

    //判断项目是否适合使用该策略：检查前置条件，用于自动发PR
    static StateFlag check(Repository repository, String repoPath, GradleStrategy strategy) {
        if(getGradleCategory(strategy)==GradleCategory.PROPERTIES){
            return checkGradleProperties(repository,repoPath,strategy)
        } else {
            return checkBuildGradle(repository, repoPath, strategy)
        }
    }

    //不判断前置条件:用于自动触发travis ci
    static StateFlag check(String repoPath,String originRepoName,GradleStrategy strategy) {
        if(getGradleCategory(strategy)==GradleCategory.PROPERTIES){
            return  GradleOptionChecker.gradleChecker(repoPath, originRepoName, strategy)
        } else {
            return BuildGradleChecker.check(repoPath, strategy)
        }
    }

    /*
        v1 < v2 返回-1
        v1 = v2 返回0
        v1 > v2 返回1
     */
    static int compareVersion(String v1, String v2) {
        //有的项目无法抽取版本号，则认为其版本==5.1.1
        if(v1 == null && v2 == null){
            return 0
        }
        if(v1 == null || v2 == null) {
            v1 = v1==null?'5.1.1':v1
            v2 = v2==null?'5.1.1':v1
        }
        int i = 0, j = 0;
        int n = v1.length(), m = v2.length();
        while(i < n || j < m)
        {
            int num1 = 0, num2 = 0;
            while(i < n && v1.charAt(i) != '.') num1 = num1 * 10 + v1.charAt(i++) - '0'.toCharacter();
            while(j < m && v2.charAt(j) != '.') num2 = num2 * 10 + v2.charAt(j++) - '0'.toCharacter();
            if(num1 > num2) return 1;
            else if( num1 < num2) return -1;
            i++; j++;
        }
        return 0;
    }

    static void main(String[] args) {
//        check(GradleStrategy.GRADLE_PARALLEL_TEST)
        //gradleLintChecker()
        //getRepoGradleVersion()
        //isMultiModule()
    }
}