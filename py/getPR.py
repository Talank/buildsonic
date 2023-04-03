import os
import json
import re
import csv
import sys
import common_unit as unit
"""
功能：根据./resources/pullRequest里面的txt文件（smell.txt）——记录该smell对应的PR_url
     生成csv文件（smell.csx)
     每个smell对应生成一个csv文件，每个csv文件包含四列数据： [PR_url, state, merged, comments]
在没有PR统计数据的时候，可以通过该程序获取smell的统计数据，然后合并
（在获得初始统计数据后，应利用incremental_update_pr.py更新统计数据）
"""

sys.path.append("/common_unit")


merged_url = []  # 记录以其它形式merge的url
map_data = unit.get_url_map()


def crawl_pr_infos(smells):
    pattern = re.compile('https://github.com/(.+)/pull/(\d+)')
    urls = []
    comment_urls = []
    for smell in smells:
        file_path = os.path.join(unit.PR_txt_dir, smell + '.txt')
        with open(file_path, "r") as file:
            for line in file:
                line = line.strip()  # https://github.com/tinkerpop/blueprints/pull/541'
                if unit.url_map(line, smell, map_data) == 'merged':
                    merged_url.append(line)   # 其它形式merged的url
                else:
                    line = unit.url_map(line, smell, map_data)  # 拆分、转移
                if 'pull' in line and 'ChenZhangg' not in line:
                    match = pattern.fullmatch(line)
                    if match is not None:
                        url = "https://api.github.com/repos/{0}/pulls/{1}".format(match.group(1), match.group(2))
                        urls.append(url)
                        comment_url = "https://api.github.com/repos/{0}/issues/{1}/comments".format(match.group(1),
                                                                                                    match.group(2))
                        comment_urls.append(comment_url)
    urls = list(set(urls))
    comment_urls = list(set(comment_urls))
    for u in urls:
        unit.get_save(u, False)

    for u in comment_urls:
        unit.get_save(u, True)


def save_csv(api_json_path, comment_json_path, out_file):
    api_json = open(api_json_path, 'r')
    data = json.load(api_json)
    api_json.close()

    comment_json = open(comment_json_path, 'r')
    com = json.load(comment_json)
    comment_json.close()
    try:
        if data['html_url'].strip() in merged_url:
            data['state'] = 'closed'
            data['merged'] = 'TRUE'
        comments = ""
        for i in com:
            comments += i['body']
            comments += '\n'
        out_file.writerow([data['html_url'], data['state'], data['merged'], comments])
    except:
        # issues
        print("404 error : " + api_json_path)
        return


def parse_json(smells):
    pattern = re.compile('https://github.com/(.+)/pull/(\d+)')
    for smell in smells:
        file_path = os.path.join(unit.PR_txt_dir, smell + '.txt')
        out_path = os.path.join(unit.PR_csv_dir, smell + 'csv')
        out_file = csv.writer(open(out_path, 'w'))
        with open(file_path, "r") as file:
            for line in file:
                line = line.strip()
                if unit.url_map(line, smell, map_data) != 'merged':
                    line = unit.url_map(line, smell, map_data)  # 拆分、转移
                if 'pull' in line and 'ChenZhangg' not in line:
                    match = pattern.fullmatch(line)
                    if match is not None:
                        commentFilePath = os.path.join(unit.PR_json_dir,
                                                       match.group(1).replace('/', '@') + '@' + match.group(
                                                           2) + '@comments' + '.json')
                        filePath = os.path.join(unit.PR_json_dir,
                                                match.group(1).replace('/', '@') + '@' + match.group(2) + '.json')
                        save_csv(filePath, commentFilePath, out_file)
        # outFile.close()


def run(smells):
    crawl_pr_infos(smells)
    parse_json(smells)


if __name__ == '__main__':
    # 在86上更新travis和Gradle
    run(unit.travis)
    run(unit.Gradle)
    # 在84上更新Maven
    run(unit.Maven)

