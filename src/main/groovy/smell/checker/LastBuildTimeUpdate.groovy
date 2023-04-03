package smell.checker

import model.Repository
import org.hibernate.Session
import org.hibernate.Transaction
import util.MysqlUtil
import util.SessionUtil

import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern
/*
Travis-ci.org
curl -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36 Edg/91.0.864.59"   -H "Authorization: token TW8mt5AdDagMyZHgOx5YQQ"  "https://api.travis-ci.org/repos/lenskit/lenskit" | sed 's/,/\n/g'  | grep "last_build_started_at"

Travis-ci.com
curl -H "Travis-API-Version: 3" -H "User-Agent: API Explorer"   -H "Authorization: token 0KJJ5DIhcjI6hju2l3qfcA"  "https://api.travis-ci.com/repo/jmeter-maven-plugin%2Fjmeter-maven-plugin/builds?limit=1" | sed 's/,/\n/g'  | grep "updated_at"

 */
class LastBuildTimeUpdate {

    /**
     * 爬取Travis-ci.org网站 API version2
     * @param repoName
     * @return command
     */
    static String getLastBuildTimeInAPI2(String repoName){
        //curl -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36 Edg/91.0.864.59"   -H "Authorization: token TW8mt5AdDagMyZHgOx5YQQ"  "https://api.travis-ci.org/repos/lenskit/lenskit" | sed 's/,/\n/g'  | grep "last_build_started_at"
        //"last_build_started_at":"2020-11-02T19:30:46Z"
        String token = "token TW8mt5AdDagMyZHgOx5YQQ"
        String host = "https://api.travis-ci.org/repos/${repoName}"
        String UserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36 Edg/91.0.864.59"
        def command = "curl -H \"User-Agent: ${UserAgent}\"   -H \"Authorization: ${token}\"  \"${host}\" | sed 's/,/\\n/g'  | grep \"last_build_started_at\""
        return command
    }

    /**
     * 爬取Travis-ci.com网站 API version3
     * @param repoName
     * @return command
     */
    static String getLastBuildTimeInAPI3(String repoName){
        //curl -H "Travis-API-Version: 3" -H "User-Agent: API Explorer"   -H "Authorization: token 0KJJ5DIhcjI6hju2l3qfcA"  "https://api.travis-ci.com/repo/jmeter-maven-plugin%2Fjmeter-maven-plugin/builds?limit=1" | sed 's/,/\n/g'  | grep "updated_at"
        //"updated_at": "2021-10-09T18:43:38.077Z"
        String Owner = repoName.split("/")[0]
        String repo = repoName.split("/")[1]
        String token = "token 0KJJ5DIhcjI6hju2l3qfcA"
        String host = "https://api.travis-ci.com/repo/${Owner}%2F${repo}/builds?limit=1"
        def command = "curl -H \"Travis-API-Version: 3\" -H \"User-Agent: API Explorer\"  -H \"Authorization: ${token}\"  \"${host}\" | sed 's/,/\\n/g'  | grep \"updated_at\""
        return command
    }

    static String getLastBuildTime(String repoName){
        String lastBuildTime = ""
        def commandAPI3 = getLastBuildTimeInAPI3(repoName)
        def commandAPI2 = getLastBuildTimeInAPI2(repoName)

        Closure TravisCrawler = { String command->
            try{
                def process = [ 'bash', '-c', command ].execute()
                process.waitFor()
                Pattern pattern = ~/(.*):(.*)T(.*)Z/
                Matcher matcher = pattern.matcher(process.text.replaceAll("\"",""))
                if(matcher.find()){
                    lastBuildTime = matcher.group(2) + " " + matcher.group(3)
                    if (lastBuildTime.contains(".")){
                        lastBuildTime = lastBuildTime.split(".")[0]
                    }
                    lastBuildTime = lastBuildTime.trim()
                    println(repoName + ":"+lastBuildTime.trim())
                    return true
                }else{
                    return false
                }
            } catch (Exception e) {
//                e.printStackTrace()
                return false
            }
        }

        boolean getLastBuildTime = TravisCrawler.call(commandAPI3)
        if (!getLastBuildTime){
            getLastBuildTime = TravisCrawler.call(commandAPI2)
        }
        if (getLastBuildTime){
            return lastBuildTime
        }
        println(repoName+":error")
        return ""
    }

    static void run(){
        try (Session session = SessionUtil.getSession()) {
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            Transaction tx = session.beginTransaction()

            for (Repository repository : repositories) {
                def lastBuildTime = getLastBuildTime(repository.repoName)
                if (lastBuildTime!=""){
                    def buildTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastBuildTime)
                    repository.setLastBuildTime(buildTime)
                    session.update(repository)
                    sleep(1000*5)
                }
            }
            tx.commit()
            session.close()
        }
    }

    static void update(){
        String lastTimeFilePath = Paths.get(System.getProperty("user.dir"), "resources","last_build_time.txt").normalize().toString()
        File lastTimeFile = new File(lastTimeFilePath)
        Map<String,String> lastTimeMap = new HashMap<>()
        lastTimeFile.readLines().each {line->
            def info = line.replaceFirst(":","@").split("@")
            lastTimeMap.put(info[0],info[1])
        }

        try (Session session = SessionUtil.getSession()) {
            List<Repository> repositories = MysqlUtil.getRepositories(session)
            Transaction tx = session.beginTransaction()

            for (Repository repository : repositories) {
                def lastBuildTime = lastTimeMap.get(repository.repoName)
                if (lastBuildTime!=null&&lastBuildTime!="error"){
                    def buildTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(lastBuildTime)
                    repository.setLastBuildTime(buildTime)
                    session.update(repository)
                }
            }
            tx.commit()
            session.close()
        }
    }

    static void main(String[] args) {
//        run()
        update()
    }
}
