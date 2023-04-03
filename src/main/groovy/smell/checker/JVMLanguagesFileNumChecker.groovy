package smell.checker

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import util.MysqlUtil
import util.SessionUtil
import util.Util

import java.nio.file.Paths

class JVMLanguagesFileNumChecker {

    static void updateJavaFilesNum() throws Exception{
        Session session = SessionUtil.getSession()
        Transaction tx = session.beginTransaction()
        //查询出表中的所有记录
        List<Repository> repositories = MysqlUtil.getRepositories(session)
        println("repositories的长度：" + repositories.size())
        int count=0;
        //遍历表中的全部记录,全部更新
        for(Repository repository : repositories){
            int JavaFilesNum = getFilesNum(repository.getRepoName())
            println(repository.getRepoName() + " JVM语言源文件数量为： " + JavaFilesNum)
            repository.setJavaFilesNum(JavaFilesNum)
            session.update(repository)
            count++
        }
        println("本次处理仓库的数量：" + count)
        tx.commit()
        session.close()
    }

    static int getFilesNum(String repoName){
        String repoPath = Paths.get(Util.codeDirectoryPath.toString(), repoName.replace('/', '@')).normalize().toString()
        File file = new File(Paths.get(repoPath).toString())
        if (file.exists()) {
            return getFilesNum(file)
        }else{
            return 0;
        }
    }

    static int getFilesNum(File file){
        int FilesNum = 0
        File[] fileList = file.listFiles()
        for (File f:fileList){
            if(f.isDirectory()){
                FilesNum += getFilesNum(f)
            }else{
                if(f.getName().endsWith(".java") || f.getName().endsWith(".groovy") || f.getName().endsWith(".kt") || f.getName().endsWith(".scala")|| f.getName().endsWith(".clj")){
                    FilesNum ++
                }
            }
        }
        return FilesNum
    }


    static void main(String[] args) {
        updateJavaFilesNum()
    }
}
