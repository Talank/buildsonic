import openpyxl
import sys
import common_unit as unit

sys.path.append("/common_unit")

'''
在调用该脚本之前，应先执行incremental_update_pr.py--->更新"PR详细信息汇总.xlsx"的数据
根据"PR详细信息汇总.xlsx"的数据，对"statistics.xlsx"的数据进行更新,更新的位置和内容如下
[ F列                          ...                                          O列   ]
[总数显, open显, closed显, 接受显 ,拒绝显, 总数隐,  open隐,  closed隐,  接受隐,  拒绝隐  ]
'''

wb_PR = openpyxl.load_workbook(unit.PR_detail_path)  # PR详细信息汇总.xlsx
wb_st = openpyxl.load_workbook(unit.statistics_path)  # statistics的副本.xlsx
EI_map = {
}


def init_EI_map(smells):
    """
    map{key:smell, value: list[PR显 , PR隐,  open显, open隐, closed显, closed隐, 接受显 , 接受隐, 拒绝显 , 拒绝隐]
    此处[open隐, closed隐]----open/close:指该PR当前的状态 ; 隐:在检测的时候是否为隐式closed
    除了MAVEN_INCREMENTAL_COMPILATION的检测情况全部为隐式OPEN，其余smell的PR均不存在检测状态为(显式/隐式)OPEN的
    :param smells:
    :return:
    """
    EI_map.clear()
    for smell in smells:
        PR_El_list = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        # [ 0      1         2        3     4       5        6        7         8       9    ]
        # [总数显, open显, closed显, 接受显 ,拒绝显,  总数隐,  open隐,  closed隐,  接受隐,  拒绝隐  ]
        try:
            sheet_data = wb_PR[smell]
        except Exception as e:
            print(smell)
            return
        column_state = 'B'  # B列：PR_state:open/close
        column_state = sheet_data[column_state]
        column_merged = 'C'  # C列：PR_merged: True/False
        column_merged = sheet_data[column_merged]
        column_EI = 'F'  # F列：PR的检测状态：显式/隐式+closed/open
        column_EI = sheet_data[column_EI]
        for line, cell in enumerate(column_EI):
            if "隐式CLOSE" in str(cell.value):
                if "open" in str(column_state[line].value):
                    PR_El_list[6] += 1  # open隐
                if "closed" in str(column_state[line].value):
                    PR_El_list[7] += 1  # closed隐
                if "True" in str(column_merged[line].value):
                    PR_El_list[8] += 1  # 接受隐
            else:
                if "open" in str(column_state[line].value):
                    PR_El_list[1] += 1  # open显
                if "closed" in str(column_state[line].value):
                    PR_El_list[2] += 1  # closed显
                if "True" in str(column_merged[line].value):
                    PR_El_list[3] += 1  # 接受显

        PR_El_list[9] = PR_El_list[7] - PR_El_list[8]  # 拒接隐 = closed隐 - 接受隐
        PR_El_list[4] = PR_El_list[2] - PR_El_list[3]  # 拒接显 = closed显 - 接受显
        PR_El_list[5] = PR_El_list[6] + PR_El_list[7]  # 总数隐 = open隐 + closed隐
        PR_El_list[0] = PR_El_list[1] + PR_El_list[2]  # 总数显 = open显 + closed显
        EI_map[smell] = PR_El_list


def run(smells, sheet_name):
    init_EI_map(smells)
    sheet = wb_st[sheet_name]
    column_smell = 'B'  # B列：smell
    column_smell = sheet[column_smell]
    for index, cell in enumerate(column_smell):
        smell = str(cell.value).strip()
        if EI_map.get(smell) is not None:
            data = EI_map.get(smell)
            # F列～O列分别为：[总数显, open显, closed显, 接受显 ,拒绝显,  总数隐,  open隐,  closed隐,  接受隐,  拒绝隐 ]
            start_column = 'F'
            for i in range(0, len(data)):
                update_cell = chr(ord(start_column) + i) + str(index + 1)  # [smell对应行,(F+i)列]
                sheet[update_cell] = data[i]


if __name__ == '__main__':
    run(unit.Travis, 'Travis')
    run(unit.Gradle, 'Gradle')
    run(unit.Maven, 'Maven')
    wb_st.save(unit.statistics_path)
