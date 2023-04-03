package util

import com.fasterxml.jackson.databind.ObjectMapper
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import model.Repository

import java.nio.file.Paths

class GithubCrawler {
    //https://github.com/i-Taozi/crawler/blob/main/info.py 复制的token
    static String token = "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"
    static String dir = Paths.get(System.getProperty("user.dir"), "resources", "lastCommit").toString()
    static crawl(String repoName) {
        String url = "${GithubUtil.API_URL}/repos/${repoName}/commits?per_page=1"
        println(url)
        def headers = ["User-Agent": "BuildPerformance",
                       "Authorization" : "token ${token}",
                       "Accept" : "application/vnd.github.v3+json"]
        def query = [per_page: 1]
        RESTClient github = new RESTClient(GithubUtil.API_URL)
        HttpResponseDecorator response = null
        try {
            response = github.get(path: "/repos/${repoName}/commits", headers: (headers), query: (query))
        } catch(HttpResponseException e) {
            e.printStackTrace()
            return
        }
        //println(response.getData())
        //println(response.getData().class)
        String date = response.getData()[0]['commit']['author']['date']
        //println(date)

        String jsonFilePath = Paths.get(dir, repoName.replaceFirst('/', "@") + '.json')
        final FileOutputStream out = new FileOutputStream(jsonFilePath);
        final ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.getData())
        new File(jsonFilePath).text = content;
    }

    static run() {
        Util.createDir(dir)
        List<Repository> repositories = MysqlUtil.getRepositories()
        for (Repository repository : repositories) {
            println(repository.getId())
            if(repository.buildTool == 1 || repository.buildTool == 2) {
                crawl(repository.getRepoName())
                //break
            }
        }
    }

    static void main(String[] args) {
        run()
    }
}
