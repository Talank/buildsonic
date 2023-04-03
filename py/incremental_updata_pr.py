import os
import json
import re
import openpyxl
import sys
import common_unit as unit

sys.path.append("/common_unit")

'''
1.根据：./resources/pullRequest/${smell}.txt文件获取该smell对应的PR_urls
2.增量式更新：对于第一步获得的PR_url,判断其是否在 ./resources/PR详细信息汇总.xlsx中
  如果不在---->属于新增PR,爬取Github.api的信息并保存为json文件，存放到./resources/pullRequest@json/中
  如果存在---->并且"PR详细信息汇总.xlsx"中该条url对应的状态为open--->爬取Github.api的信息为json文件，更新到./resources/pullRequest@json/中
3.根据以上两类URL的json文件，更新"PR详细信息汇总.xlsx"
  [ A       B       C       D          E       F   ]
  [PR_url，state，merged，comments, 拒绝原因,  显/隐式]
  第二步获得的json文件可以提供前四列的信息
  新增：新增一行，额外判断是显式/隐式CLOSE
  更新：修改原来的行的2/3/4列
'''


PR_detail_data = openpyxl.load_workbook(unit.PR_detail_path)
merged_url = []  # 记录以其它形式merge的url---changedURL.json
map_data = unit.get_url_map()

def crawl_pr_infos(file_name):
    """
    根据./resources/pullRequest/${smell}.txt，获取当前该smell的PR_url信息
    抽取其中的 [new_url, update_url]，并记录这些PR所对应的api.github信息，通过调用 get_save(url, isComments)，保存json数据
    :param file_name: 例如travis_cache.txt
    :return: [new_url, update_url]
    """
    smell = file_name.split('.')[0].upper()  # 根据file_name获取对于的smell, travis是小写的，需要改成大写
    pattern = re.compile('https://github.com/(.+)/pull/(\d+)')
    api_urls = []  # 存放api.github/repo/{owner/repo}/pulls/{numbers}的url
    comment_urls = []  # 存放api的comment的url
    new_url = []  # 新增的PR(之前不存在于'PR详细信息汇总.xlsx'的PR)
    update_url = []  # 更新的PR(之前状态为open的PR)
    [open_urls, closed_urls] = get_openAndClosed_urls(smell)  # 当前'PR详细信息汇总.xlsx'中，状态分别为open/closed的PR
    all_urls = closed_urls + open_urls  # 当前'PR详细信息汇总.xlsx'中，所有的PR
    file_path = os.path.join(unit.PR_txt_dir, file_name)
    with open(file_path, "r") as file:
        for line in file:
            line = line.strip()  # https://github.com/tinkerpop/blueprints/pull/541'
            if unit.url_map(line, smell, map_data) == 'merged':
                merged_url.append(line)  # 其它形式merged的url
            else:
                line = unit.url_map(line, smell, map_data)  # 拆分、转移
            if 'pull' in line and 'ChenZhangg' not in line:
                if line not in all_urls:
                    new_url.append(line)  # 如果该url不在当前统计信息中，则为新增的PR
                if line in open_urls:
                    update_url.append(line)  # 如果该url在当前统计信息中，并且状态为open，则为更新的PR
                if line in closed_urls:
                    continue
                match = pattern.fullmatch(line)
                if match is not None:
                    url = "https://api.github.com/repos/{0}/pulls/{1}".format(match.group(1), match.group(2))
                    api_urls.append(url)
                    comment_url = "https://api.github.com/repos/{0}/issues/{1}/comments".format(match.group(1),
                                                                                                match.group(2))
                    comment_urls.append(comment_url)

    api_urls = list(set(api_urls))
    comment_urls = list(set(comment_urls))
    for u in api_urls:
        unit.get_save(u, False)

    for u in comment_urls:
        unit.get_save(u, True)
    return [new_url, update_url]


def get_openAndClosed_urls(smell):
    """
    根据./resources/PR详细信息汇总.xlsx，获取已经在统计表格中的url
    :param smell: smell作为"PR详细信息汇总.xlsx"的sheet名
    :return: [open_urls[], closed_urls[] ]
    """
    smell = str(smell).upper()  # travis_smell.txt 小写--->sheet名都是大写
    sheet = PR_detail_data[smell]
    closed_urls = []
    open_urls = []
    column_url = 'A'  # A列为PR_url
    column_url = sheet[column_url]
    column_state = 'B'  # B列为PR的状态：open/closed
    column_state = sheet[column_state]
    for index, line in enumerate(column_state):
        cell = str(line.value)
        url = str(column_url[index].value).strip()
        if "open" in cell:
            open_urls.append(url)
        else:
            closed_urls.append(url)
    return [open_urls, closed_urls]


def get_json_info(api_json_path, comment_json_path):
    """
    :param api_json_path: ./resources/pullRequest@json/owner@repo@pullsNumber.json
    :param comment_json_path: ./resources/pullRequest@json/owner@repo@pullsNumber@comments.json
    :return:  [ data['html_url'], data['state'], data['merged'], comments]
    """
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
        # issues
        comments = ""
        for i in com:
            comments += i['body']
            comments += '\n'
        return [data['html_url'], data['state'], data['merged'], comments]
    except:
        # issues
        print("404 error : " + api_json_path)
        return


def parse_json(smell, new_url, update_url):
    """
    :param smell:
    :param new_url: 新增的PR
    :param update_url: 更新的PR
    :return:
    """
    pattern = re.compile('https://github.com/(.+)/pull/(\d+)')
    for url in update_url + new_url:
        if 'pull' in url and 'ChenZhangg' not in url:
            match = pattern.fullmatch(url)
            if match is not None:
                comment_json_path = os.path.join(unit.PR_json_dir, match.group(1).replace('/', '@') + '@' + match.group(
                    2) + '@comments' + '.json')
                api_json_path = os.path.join(unit.PR_json_dir,
                                             match.group(1).replace('/', '@') + '@' + match.group(2) + '.json')
                info_json = get_json_info(api_json_path, comment_json_path)
                if url in update_url:
                    update_xlsx(smell, True, info_json)
                else:
                    update_xlsx(smell, False, info_json)


def update_xlsx(smell, is_update, info):
    """
    :param smell: 对应'PR详细信息汇总.xlsx'中的sheet名
    :param is_update: True:更新的PR;  False:新增的PR
    :param info: [url, state, merged, comments]
    :return:
    """
    sheet = PR_detail_data[smell]
    if is_update:
        # 如果是更新某一行
        column_url = 'A'  # A列：PR_url
        column_url = sheet[column_url]
        for index, cell in enumerate(column_url):
            if str(cell.value).strip() == info[0]:
                state = "B" + str(index + 1)  # B列：state
                merged = "C" + str(index + 1)  # C列： merged
                comments = "D" + str(index + 1)  # D列： comments
                sheet[state] = info[1]
                sheet[merged] = info[2]
                sheet[comments] = info[3]
            break
    else:
        # 如果是新增一行
        EI_state = get_EI(info[0], smell)
        sheet.append([info[0], info[1], info[2], info[3], " ", EI_state])  # " "为拒绝原因，暂时不填


def get_EI(url, smell):
    """
    :param url: PR_url
    :param smell:
    :return:EI_state: 显式/隐式+closed/open
    """
    EI_state = None
    EI_path = os.path.abspath(os.path.join(unit.EI_dir, smell + '@EI.txt'))
    with open(EI_path, 'r') as f:
        EI_data = f.readlines()
        for index in range(0, len(EI_data)):
            EI_data[index] = EI_data[index].strip()
            if '式' in EI_data[index]:
                EI_state = EI_data[index]
            if url in EI_data[index]:
                return EI_state


def run(smells):
    for smell in smells:
        file_name = smell + '.txt'
        [new_url, update_url] = crawl_pr_infos(file_name)
        smell = smell.upper()  # travis的smell--->大写
        parse_json(smell, new_url, update_url)
    PR_detail_data.save(unit.PR_detail_path)


if __name__ == '__main__':
    # 在86上更新travis和Gradle
    run(unit.travis)
    run(unit.Gradle)
    # 在84上更新Maven
    run(unit.Maven)
