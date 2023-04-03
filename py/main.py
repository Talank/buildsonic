# This is a sample Python script.

# Press ⌃R to execute it or replace it with your code.
# Press Double ⇧ to search everywhere for classes, files, tool windows, actions, and settings.
import bashlex
import re

def handle_script(content):
    """
    预处理脚本内容，去除语法解析器无法解析的内容
    :param content:
    :return:
    """
    # 去除掉注释
    # content = re.sub(r'^[\s]*?#[\s\S]*?\r\n', '', content)
    content = re.sub(r'#+[\s|\S]*?\n', '\r\n', content)
    # content = re.sub(r'\r\n\r\n', '\r\n', content)
    # 处理<<EOF EOF的问题
    content = re.sub(r'[\s]*?<<[\s]*?EOF[\s|\S]*?EOF', '', content)
    # 去除case语句
    content = re.sub(r'case[\s|\S]*?esac', 'echo "aaa"', content)
    # 提取while语句中的内容,去除while[]do done外壳，将内容放到内容中进行检测
    while_re_str = r'while[\s|\S|\r\n|\r]*?do([\s|\S|\r\n|\r]*?)done'
    find_list = re.findall(while_re_str, content)
    for item in find_list:
        content = re.sub(while_re_str, item, content, 1)

    # 去除[ $之间的空格
    content = re.sub('\[\s+\$', '[$', content)
    # 去除[ "$之间的空格
    content = re.sub('\[\s+"\$', '["$', content)
    # 去除[ `之间的空格
    content = re.sub('\[\s+`', '[`', content)

    # 带括号不支持
    # 处理do\r的问题
    content = re.sub(r'do\r', 'do \r\n', content)


    # \r\n变成\n
    content = re.sub(r'\r\n', '\n', content)
    # 处理每行末尾空格的情况
    content = re.sub(r'[\s]*?\n', '\n', content)

    # 去除空行
    content = re.sub(r'[\n]{2,}', '\n', content)

    content = content.strip()
    # print(repr(content))
    #print(content)
    return content

def print_hi(name):
    with open("testResource/1.sh", "r") as text_file:
        s = text_file.read()
    #print(s)
    parts = bashlex.parse(handle_script(s))
    for ast in parts:
        print(ast.dump())
    # Use a breakpoint in the code line below to debug your script.
    print(f'Hi, {name}')  # Press ⌘F8 to toggle the breakpoint.


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    print_hi('PyCharm')

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
