package util

import groovy.json.JsonBuilder
import groovyx.net.http.ContentType
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import java.nio.file.Paths

class GithubUtil {
    static String API_URL = "https://api.github.com"
    static Properties properties = new Properties()

    static String token
    static String USER_NAME
    static {
        properties.load(new FileReader(Paths.get(System.getProperty("user.dir"), "userInfo.properties").normalize().toString()))
        token = properties.get("token")
        USER_NAME = properties.get("USER_NAME")
    }

    static void forkRepo(String fullRepoName) {
        def headers = ["User-Agent": "BuildPerformance",
                       "Authorization" : "token ${token}",
                        "Accept" : "application/vnd.github.v3+json"]
        RESTClient github = new RESTClient(API_URL)
        retry {
            HttpResponseDecorator response = github.post(path: "/repos/${fullRepoName}/forks", headers: (headers))
            response.status
        }
    }

    static void retry(int times = 5, Closure errorHandler = {e -> e.printStackTrace}
                     , Closure body) {
        int retries = 0
        def exceptions = []
        while(retries++ < times) {
            try {
                int status = body.call()
                if (status == 200 || status == 201 || status == 202 || status == 204) {
                    return
                } else {
                    println(status)
                    sleep(10000)
                }
            } catch(HttpResponseException e) {
                e.printStackTrace()
                return
            }

        }
        throw new Exception("Failed after $times retries", exceptions)
    }

    static void deleteRepo(String fullRepoName) {
        def headers = ["User-Agent": "BuildPerformance",
                       "Authorization" : "token ${token}",
                       "Accept" : "application/vnd.github.v3+json"]
        RESTClient github = new RESTClient(API_URL)
        retry {
            HttpResponseDecorator response = github.delete(path: "/repos/${fullRepoName}", headers: (headers))
            println(response.status)
            println(response.success)
            println(response.getData())
            response.status
        }
    }

    static Boolean pullRequest(String repoName, String head, String base, String title, String description, String outFilePath) {
        println("调用pullRequest方法")
        println(head)
        println(repoName)
        def headers = ["User-Agent": "BuildPerformance",
                       "Authorization": "token ${token}",
                       "Accept": "application/vnd.github.v3+json",
                       ]
        def body = ["title": (title),
                     "body": (description),
                     head: (head),
                     base: (base)]
        RESTClient github = new RESTClient(API_URL)
//        github.handler.failure = { resp, reader ->
//            println(resp.getData())
//            println(resp.status)
//            println(reader)
//        }
        Boolean PRCreated = false
        retry {
            HttpResponseDecorator response = github.post(path: "/repos/${repoName}/pulls", headers: (headers), body: (body), requestContentType: ContentType.JSON)
            String html_url = response.getData()["html_url"]
            println(html_url)
            if(html_url.startsWith("https")){
                PRCreated = true
            }
            new File(outFilePath).withWriterAppend() { writer ->
                writer << html_url << "\n"
            }
//            outFilePaths.each { outFilePath ->
//                new File(outFilePath).withWriterAppend() { writer ->
//                    writer << html_url << "\n"
//                }
//            }
            response.status
        }
        return PRCreated
    }

    static String getDefaultBranchName(String fullRepoName) {
        def headers = ["User-Agent": "BuildPerformance",
                       "Authorization" : "token ${token}",
                       "Accept" : "application/vnd.github.v3+json"]
        String branchName = "master"
        RESTClient github = new RESTClient(API_URL)
        retry {
            HttpResponseDecorator response = github.get(path: "/repos/${fullRepoName}", headers: (headers))
            branchName = response.getData()["default_branch"]
            response.status
        }
        return branchName
    }

    static void crawlMVNRepository(String url, String outFilePath) {
        try {
            String content = new URL(url).getText([connectTimeout: 10000, readTimeout: 10000])
            File file = new File(outFilePath)
            file.text = content
        } catch(Exception e){
            e.printStackTrace()
        }
    }
}
