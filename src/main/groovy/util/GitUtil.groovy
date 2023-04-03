package util

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.api.errors.RefNotFoundException
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.StoredConfig
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

class GitUtil {
    static void cloneRepo(int times = 5, String localPath, String fullRepoName) {
        //String remoUrl = "https://github.com/${fullRepoName}.git"
        //String remoUrl = "git://github.com/${fullRepoName}.git"
        String remoUrl = "git@github.com:${fullRepoName}.git"
        int retries = 0
        while(retries++ < times) {
            Git result = null
            try {
                result = Git.cloneRepository()
                        .setURI(remoUrl)
                        .setDirectory(new File(localPath))
                        .call()
                System.out.println("Clone repository: " + result.getRepository().getDirectory());
            } catch(TransportException e) {
                sleep(5000)
                e.printStackTrace()
            } catch(Exception e) {
                e.printStackTrace()
            } finally {
                result?.close()
            }
            if (result != null)
                break
        }
    }

    static Repository openRepo(String repoPath) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File(repoPath))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build()
        return repository
    }

    static String createAndCheckoutBranch(String repoPath, String branchName, String defaultBranchName) {
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        return createAndCheckoutBranch(git, branchName, defaultBranchName)
    }

    static void checkoutToDefaultBranch(String repoPath, String originRepoName){
        def defaultBranchName= GithubUtil.getDefaultBranchName(originRepoName)
        println("默认分支： "+defaultBranchName)
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        git.checkout().setName(defaultBranchName).call()
    }

    /**
     * 如果本地分支存在，切换到该分支，并且返回true
     * 如果远程分支存在，checkout -t下载远程分支，并且返回true
     * 如果不存在该分支，则在当前分支基础上创建新的分支，并且返回false
     * 其它情况返回null
     * @param repoPath
     * @param branchName : 切换的分支
     * @param baseBranch : 创建新分支时的Base Branch
     * @return
     */
    static Boolean CheckoutAndCreate(String repoPath, String branchName, String baseBranch){
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        def (localBranches, remoteBranches) = getBranches(repoPath)
        Closure checkoutExecute = { String branch, String base->
            try{
                if (base==null){
                    git.checkout().setName(branch).call()
                }else{
                    git.checkout().setStartPoint("origin/" + base).setCreateBranch(true).setName(branch).call()
                }
                return true
            } catch (Exception e){
                println(e.getClass().simpleName)
            }
            return false
        }


        if (remoteBranches.contains(branchName)){
            // 如果存在远程分支
            if (localBranches.contains(branchName)){
                // 并且本地也存在该分支，直接本地切换
                return checkoutExecute.call(branchName,null)
            }else{
                // 如果本地没有该分支，则远程克隆并切换
                println("远程分支存在，本地分支不存在")
                return checkoutTOriginBranch(git,branchName)
            }
        }else if(localBranches.contains(branchName)){
            // 如果不存在远程分支，但是存在本地分支：no travis或者下载code出错的
            println("origin本地分支存在，但远程分支不存在")
            return null
        }

        // 如果远程和本地都不存在分支，创建新的分支
        def ok = checkoutExecute.call(branchName,baseBranch)
        if(ok){
            return false  // false表示成功创建新的分支
        }else{
            return null   // 创建新分支失败
        }
    }

    /**
     * 等价于： git checkout -t origin/branchName
     * 功能：在本地clone remote branch并建立联系，本地分支名和远程分支名相同
     * @param branchName
     * @return
     */
    static Boolean checkoutTOriginBranch(Git git, String branchName){
        try{
            git.checkout().setCreateBranch(true).setName(branchName).
                    setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                    setStartPoint("origin/" + branchName).call()
            return true
        } catch (Exception e){
            println(e.getClass().simpleName)
        }
        return false
    }

    /**
     * 返回本地存在的分支和远程分支 （分支名）
     * @param git
     * @return [localBranches, remoteBranches]
     */
    static List<List<String>> getBranches(String repoPath){
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        List<Ref> Refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call()
        def localBranches = []
        def remoteBranches = []
        Pattern localPattern = ~/refs\/heads\/(.+)/
        Pattern remotePattern = ~/refs\/remotes\/origin\/(.+)/

        Refs.each {ref->
            Matcher localMatcher = localPattern.matcher(ref.getName())
            Matcher remoteMatcher = remotePattern.matcher(ref.getName())
            if (localMatcher.matches()) {
                localBranches << localMatcher.group(1)
            } else if (remoteMatcher.matches()){
                remoteBranches << remoteMatcher.group(1)
            } else{
                println("其它情况"+ref.getName())
            }
        }
        return [localBranches, remoteBranches]

    }

    static String createAndCheckoutBranch(Git git, String strategyName, String defaultBranchName) {
        List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        List<String> branchNames = []
        Pattern pattern = ~/refs\/(heads|remotes\/origin|remotes\/upstream)\/(.+)/
        refs.each {
            Matcher matcher = pattern.matcher(it.getName())
            if (matcher.matches()) {
                branchNames << matcher.group(2)
            }
        }
        println(branchNames)
        String branchName = null
        int count = 0
        Pattern countPattern = ~/${strategyName}_(\d+)/

        for (String bn in branchNames) {
            if (bn.contains(strategyName) && bn ==~ countPattern) {
                println(bn)
                Matcher countMatcher = countPattern.matcher(bn)
                if (countMatcher) {
                    int tmpCount = countMatcher[0][1] as int
                    count = tmpCount > count ? tmpCount : count
                }
            }
        }
        count++
        branchName = "${strategyName}_${count}"
        println("产生新分支: " + branchName)
        git.checkout().setStartPoint("upstream/${defaultBranchName}").setCreateBranch(true).setName(branchName).call()
        return branchName
    }

    static void addAndCommit(String repoPath, String commitMessage) {
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        Status status = git.status().call();
        System.out.println("Untracked: " + status.getUntracked().size())
        System.out.println("Added: " + status.getAdded().size())
        System.out.println("Changed: " + status.getChanged().size())
        System.out.println("Modified: " + status.getModified().size())
        System.out.println("Removed: " + status.getRemoved().size())

        List<String> changedFiles = ([] << status.getUntracked() << status.getAdded() << status.getChanged() << status.getModified() << status.getRemoved()).flatten()
//        println(changedFiles)
        if (changedFiles.size() > 0) {
            addAndCommit(git, changedFiles, commitMessage)
        }
    }
    static void addAndCommit(Git git, List<String> fileList, String commitMessage) {
        // run the add
        fileList.each {
            git.add().addFilepattern(it).call();
        }

        // and then commit the changes
        git.commit().setMessage(commitMessage).call();
    }

    //如果没有就设置upstream
    static void setUpstream(String repoPath, String upstreamRepoName) {
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        StoredConfig config = repository.getConfig()
        String value = config.getString("remote","upstream", "url")
        if (value == null) {
            config.setString("remote","upstream", "url", "git@github.com:${upstreamRepoName}.git")
            config.setString("remote","upstream", "fetch", '+refs/heads/*:refs/remotes/upstream/*')
            config.save()
        }

    }

    static void deleteAllFiles(String repoPath){
        // delete一个GitHub仓库的所有内容，只保留.git
        Repository repository = openRepo(Paths.get(repoPath, ".git").toString())
        Git git = new Git(repository)
        File repo = new File(repoPath)
        repo.listFiles().each {
            if (it.name!='.git'){
                git.rm().addFilepattern(it.name).call()
            }
            if (it.name!='.git' && it.exists()){
                def rm = "rm -rf ${it.absolutePath}".execute(null,new File(repoPath))
                rm.waitFor()
            }
        }
        sleep(1000*10)  // 延迟10s
    }
}
