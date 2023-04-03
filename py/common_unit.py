import requests
import os
import json
import re

"""
主要分为三部分：
1. 路径
2. smell
   其中travis ci分为大写和小写（在统计数据中，对应的smell均为大写，./resources/pullRequest中的记录txt文件为小写
3. 爬取Github.API和保存爬取数据等方法
"""

# 1.路径
base_dir = os.path.abspath(os.path.join('..'))  # BuildPerformance
resources_dir = os.path.abspath(os.path.join(base_dir, 'resources'))  # BuildPerformance/resources

# 存放smell@EI.txt 记录repo对应该smell是显式或隐式
EI_dir = os.path.abspath(os.path.join(resources_dir, 'EI'))  # BuildPerformance/resources/EI
PR_json_dir = os.path.abspath(os.path.join(resources_dir, 'pullRequest@json'))
PR_txt_dir = os.path.abspath(os.path.join(resources_dir, 'pullRequest'))
PR_csv_dir = os.path.abspath(os.path.join(resources_dir, 'pullRequest@csv'))

PR_detail_path = os.path.abspath(os.path.join(resources_dir, 'PR统计信息.xlsx'))
statistics_path = os.path.abspath(os.path.join(resources_dir, 'statistics.xlsx'))

# 2.smell
gradle_property = ['CACHING', 'CONFIGURATION_ON_DEMAND', 'FILE_SYSTEM_WATCHING', 'PARALLEL_BUILDS', 'GRADLE_DAEMON']
gradle_build = ['GRADLE_COMPILER_DAEMON', 'GRADLE_INCREMENTAL_COMPILATION', 'GRADLE_PARALLEL_TEST',
                'GRADLE_REPORT_GENERATION', 'GRADLE_FORK_TEST']
Gradle = gradle_property + gradle_build

maven_test = ['MAVEN_PARALLEL_TEST', 'MAVEN_REPORT_GENERATION', 'MAVEN_FORK_TEST']
maven_compile = ['MAVEN_PARALLEL_EXECUTION', 'MAVEN_COMPILER_DAEMON']
Maven = maven_test + maven_compile

Travis = ['TRAVIS_CACHE', 'TRAVIS_FAST_FINISH', 'TRAVIS_RETRY', 'TRAVIS_SHALLOW_CLONE', 'TRAVIS_WAIT']
all_smells = Gradle + Maven + Travis


# 3.方法

# 爬取Github.API

def get_response(url):
    headers = {'User-Agent': 'Mozilla/5.0',
               'Authorization': 'token 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',
               'Content-Type': 'application/json',
               'Accept': 'application/json',
               'method': 'GET'
               }
    try:
        response = requests.get(url, headers=headers)
        return response
    except requests.exceptions.RequestException as e:
        return None


# 以json形式保存爬取Github.API的数据
def get_save(url, isComments):
    if isComments:
        match = re.match('https://api.github.com/repos/(.+)/issues/(\d+)/comments', url)
        filePath = os.path.join(PR_json_dir,
                                match.group(1).replace('/', '@') + '@' + match.group(2) + '@comments' + '.json')
    else:
        match = re.match('https://api.github.com/repos/(.+)/pulls/(\d+)', url)
        filePath = os.path.join(PR_json_dir, match.group(1).replace('/', '@') + '@' + match.group(2) + '.json')
    print(filePath)
    response = get_response(url)
    if response is not None:
        json_file = json.dumps(response.json(), indent=5)
        f = open(filePath, 'w')
        f.write(json_file)
        f.close()


# 记录转移、拆分、其它形式merge的PR
def get_url_map():
    """
    读取changedURL.json
    防止出现：其它程序短时间内大量调用url_map,每次调用url_map时都需要重新打开changedURL.json
    抽取这部分IO作为单独的一个方法
    :return: map_data
    """
    map_path = os.path.abspath(os.path.join(resources_dir, 'changedURL.json'))
    map_file = open(map_path, 'r', encoding='utf8')
    map_data = json.load(map_file)
    map_file.close()
    return map_data


def url_map(url, smell, map_data):
    """
    根据changedURL.json判断该url是否为拆分、转移或者其它形式merged的url
    如果为拆分或转移，返回新的url
    如果为其它形式merged，返回"merged"
    如果不属于上述三种，返回原本的url
    """
    for item in map_data:
        if item['URL'].strip() == url:
            for replace in item['replace']:
                if replace['category'] == 'split' and smell in replace['smell']:
                    return replace['newURL']
                if replace['category'] == 'transfer':
                    return replace['newURL']
                if replace['category'] == 'merged':
                    return 'merged'
    return url
