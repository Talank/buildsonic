import requests
from bs4 import BeautifulSoup
import bs4
import os

# dirPath是存放依赖抽取文件的地址
dirPath = r"F:\Maven01\TestProject\pythonTestC"

def getResponse(url):
    header = {
        "user-agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36",
    }
    try:
        # 通过requests的get方法获得源代码
        response = requests.get(url, headers=header)
        # 判断返回状态码是否为200，不为200直接进入异常
        # response.raise_for_status()
        # 打印头部信息看看，可注释掉
        # print(r.request.headers)
        # r.encoding = r.apparent_encoding # 根据情况是否填写
        return response.text
    except:
        print("爬取失败！")
        return " "


def get_name(dependencyName):
    '''
    这个函数要根据具体的依赖抽取的格式来定
    将传入的Dependency的name分成三部分
    :return:
    :param dependencyName:例如"Name: com.alibaba.cola/cola-component-domain-starter, VersionContent: ${cola.framework.version} , Version: 4.1.0-SNAPSHOT"
    :return:  [name, versionContent, verison] = [com/alibaba/cola/cola-component-domain-starter, cola.framework.version, 4.1.0-SNAPSHOT]
    '''
    items = dependencyName.split(",")
    name = items[0]
    name = name.split(":")[1].strip().replace(".", "/")
    versionContent = items[1]
    versionContent = versionContent.split(":")[1].strip()
    version = items[2]
    version = version.split(":")[1].strip()
    return [name, versionContent, version]


def parser(response, version):
    latest = ""
    release = ""
    versionList = []
    for line in response.splitlines():
        line = line.strip()
        if line.startswith("<latest>"):
            latest = line[8:-9]
        elif line.startswith("<release>"):
            release = line[9:-10]
        elif line.startswith("<version>"):
            versionList.append(line[9:-10])

    if version == "1.0.0-SNAPSHOT":
        return latest
    if version == "LATEST" and latest != "":
        return latest
        # return latest
    if version == "RELEASE" and release != "":
        return release
        # return release

    if version.endswith("SNAPSHOT"):
        newVersion = version[0:-9]
        if newVersion in versionList:
            return newVersion
        elif newVersion.replace(".", "").isdigit():
            newNum = int(newVersion.replace(".", ""))
            for v in versionList:
                if v.replace(".", "").isdigit():
                    vNum = int(v.replace(".", ""))
                else:
                    vNum = int(v.split('-')[0].replace(".", ""))

                if vNum >= newNum:
                    return v
        return latest


def run(fileName):
    filePath = os.path.join(dirPath, fileName)
    # 测试，data是指爬取版本后的保存文件，通过print直接输出到该文件
    data = open("modify_" + fileName, 'w', encoding="utf-8")
    POMs = getPOMs(filePath)
    if len(POMs) == 0:
        return
    for POM in POMs:
        print(POM[0], file=data)
        print(POM[1], file=data)
        for dependencyName in POM[2:]:
            [name, versionContent, verison] = get_name(dependencyName)
            # if versionContent == "${project.version}" or versionContent == "${parent.version}" or versionContent == "${parent.project.version}":
            #     continue
            url = "https://repo1.maven.org/maven2/" + name + "/maven-metadata.xml"
            #print("url: " + url)
            response = getResponse(url)
            if response is None:
                print(dependencyName + "爬取失败，没有找到对应构件", file=data)
            else:
                print("dependency: {" + dependencyName + " }", end="", file=data)
                newVersion = parser(response, verison)
                print("  ------->" + newVersion, file=data)
        print("\n", file=data)


def getPOMs(filePath):
    file = open(filePath, "r", encoding='ISO-8859-1')
    content = []
    for line in file:
        line = line.strip()
        if line != "":
            content.append(line)
    file.close()
    POMs = []
    for i in range(0, len(content)):
        if content[i].startswith("POM{"):
            POM = []
            while i < len(content):
                POM.append(content[i])
                i = i + 1
                if content[i].startswith('-'):
                    if len(POM) > 2:
                        POMs.append(POM)
                    break

    # 将version为project.version的依赖去掉
    iPOMs = []
    for POM in POMs:
        iPOM = []
        iPOM.append(POM[0])
        iPOM.append(POM[1])
        for string in POM[2:]:
            if "${project.version}" not in string:
                iPOM.append(string)
        if len(iPOM) > 2:
            iPOMs.append(iPOM)

    print("iPOMs_len：" + str(len(iPOMs)))
    return iPOMs



if __name__ == "__main__":

    fileList = os.listdir(dirPath)
    for fileName in fileList:
        print("projectName： " + fileName)
        run(fileName)
        print("\n")
