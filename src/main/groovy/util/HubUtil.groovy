package util

import groovy.json.JsonSlurper

class HubUtil {
    static List<String> getRepoList() {
        String command = "hub api -X GET /user/repos"
        def text = command.execute().text
        def repoInfos = new JsonSlurper().parseText(text)
        def result = []
        repoInfos.each { m ->
            result.add(m["name"])
        }
        return result
    }

    static void deleteRepo(String repoName) {
        if(repoName =~ /_[0-4]/) {
            println(repoName)
            String command = "hub delete -y $repoName"
            def p = command.execute()
            p.waitFor()
        }
    }

    static void clearRepos() {
        List<String> repoList = getRepoList()
        repoList.each {
            deleteRepo(it)
        }
    }

    static int cloneRepo(String repoPath, String fullRepoName) {
        def cloneProcess = "git clone git@github.com:${fullRepoName}.git ${repoPath}".execute()
        cloneProcess.waitForProcessOutput(System.out, System.err)
        return cloneProcess.exitValue()
    }

    static int push(String repoPath, String branchName) {
        def pushProcess = "git push origin ${branchName}".execute(null, new File(repoPath))
        pushProcess.waitFor()
        println(pushProcess.err.text)
        return pushProcess.exitValue()
    }

    static int pullRequest(String repoPath, String userName) {
        println(userName)
        println(repoPath)
        def pullProcess = "hub pull-request -b ${userName}:master -m \"title\" -m \"body\"".execute(null, new File(repoPath))
        pullProcess.waitFor()
        println(pullProcess.text)
        println(pullProcess.err.text)
    }

    static void forkRepo(String repoPath) {
        int retries = 0
        while(retries++ < 5) {
            def forkProcess = 'hub fork --remote-name origin'.execute(null, new File(repoPath))
            forkProcess.waitFor()
            println(forkProcess.err.text)
            println(forkProcess.exitValue())
            if (forkProcess.exitValue() == 0) {
                return
            } else {
                sleep(5000)
            }
        }
    }

    //
    static int fetchAndMergeUpStream(String repoPath, String defaultBranchName = "master") {
        def fetchProcess = "git fetch upstream".execute(null, new File(repoPath))
        fetchProcess.waitForProcessOutput(System.out, System.err)
        return fetchProcess.exitValue()
        //def mergeProcess = "git merge upstream/${defaultBranchName} ${defaultBranchName}".execute(null, new File(repoPath))
        //mergeProcess.waitForProcessOutput(System.out, System.err)
        //return mergeProcess.exitValue()
    }

}
