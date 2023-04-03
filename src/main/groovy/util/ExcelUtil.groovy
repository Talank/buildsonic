package util

import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook


/**
 * 读取excel文件
 */
class ExcelUtil {
    private XSSFWorkbook Workbook = null
    XSSFSheet sheet = null
    String excelPath
    ExcelUtil(String excelPath){
        this.excelPath = excelPath
        try {
            this.Workbook = new XSSFWorkbook(new FileInputStream(excelPath))
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    ExcelUtil(String excelPath, String sheetName){
        try {
            this.excelPath = excelPath
            this.Workbook = new XSSFWorkbook(new FileInputStream(excelPath))
            this.sheet = Workbook.getSheet(sheetName)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }


    List<XSSFSheet> getSheets(){
        List<XSSFSheet> sheets = new ArrayList<>()
        for (int i = 0; i < Workbook.getNumberOfSheets(); i++) {//获取每个Sheet表
            sheets << Workbook.getSheetAt(i)
        }
        return sheets
    }

    String getCell(int row, int column){
        def Row = this.sheet.getRow(row)
        if(Row==null){
            return ""
        }
        def value = Row.getCell(column).toString()
        if(value==null || value.trim()=="" || value.trim()=="null"){
            return ""
        }
        return value
    }
    
    List<String> getCells(int column){
        List<String> cells = new ArrayList<>()
        for(int index = sheet.firstRowNum; index <= sheet.lastRowNum ; index++){
            String cell = getCell(index, column).toString()
            if(cell != ""){
                cells << cell
            }
        }
        return cells
    }


    void addCellWithContent(int raw, int column, String content){
        XSSFRow row = sheet.createRow(raw)
        XSSFCell cell = row.createCell(column)
        cell.setCellValue(content)
    }

    /**
     * 该方法仅适用于'PR时间对比.xlsx'
     * 给定smell(strategy),定位到'PR时间对比.xlsx'的具体sheet
     * 然后根据repoName，找到对应的行 raw  在cell(raw,column)上写入内容content
     * TravisAPIUtil中的static方法parserBuildsInfo()调用了该方法，使用synchronized关键字保证写互斥
     */
    boolean addContentByRepoName(String smell, String repoName, Map<String,String> newContents){
        // column:6\7\8分别对应'PR详细信息汇总.xlsx'中的G\H\I列
        Map<String,Integer> columnMap = ["originTime":6, "fixedTime":7,"improvement":8]

        this.sheet = Workbook.getSheet(smell)

        def repos = getCells(0)
        def raw = repos.findIndexOf {repoInfo->
            if(repoInfo.contains(repoName)){
                return true
            }
        }

        newContents.each {columnName,content->
            def column = columnMap.get(columnName)
            if(column==null){
                println("没有找到具体的column")
                return false
            }
            addCellWithContent(raw,column,content)
            //
            if(columnName=='fixedTime' && content.isInteger()){
                def originTime = getCell(raw,6)
                if(originTime.isInteger()){
                    def improvement = 100*(originTime.toInteger() - content.toInteger())/originTime.toInteger()
                    addCellWithContent(raw,8,improvement.toString())
                }
            }
        }

        try {
            def out = new FileOutputStream(excelPath)
            this.Workbook.write(out)
            out.close()
            return true
        } catch (Exception e){
            e.printStackTrace()
            return false
        }
    }
        /**
     * 按行遍历sheet，如果row能满足columnMap的所有元素，即(row,columnIndex)的值都等于columnValue
     * 则将该row加入List<XSSFRow> rows
     * @param columnMap{key:columnIndex ; value:columnValue }
     * @return List<XSSFRow> rows
     */
    List<XSSFRow> getRowsConditionally(Map<Integer,String> columnMap){
        List<XSSFRow> rows = new ArrayList<>()
        for(int index = sheet.firstRowNum; index <= sheet.lastRowNum ; index++){
            if (columnMap.every {getCell(index, it.key) == it.value}){
                rows << sheet.getRow(index)
            }
        }
        return rows
    }

    /**
     * 按行遍历sheet，如果row能满足columnMap的所有元素，即(row,columnIndex)的值都等于columnValue
     * 则将该row和column对应的cell加入 List<String> cells
     * @param columnMap{key:columnIndex ; value:columnValue } , column返回的cell列号
     * @return List<String> cells
     */
    List<String> getCellsConditionally(Map<Integer,String> columnMap, int column){
        if(columnMap.size()==0){
            return null
        }
        List<XSSFRow> rows = getRowsConditionally(columnMap)
        List<String> cells = new ArrayList<>()
        rows.each {
            cells << it.getCell(column).toString()
        }
        return cells
    }

    static void main(String[] args) {
        assert (0.0..1.0).containsWithinBounds(0.5)
        // column下标从0开始
//        String filePath = "/Users/hujunhao/Code/github/BuildPerformance/resources/PR详细信息汇总.xlsx"
//        ExcelUtil excel = new ExcelUtil(filePath, "TRAVIS_WAIT")

//        def cells = excel.getCells(2)
//        cells.each {println it}


//        Map<Integer,String> columnMap = [
//            2: "TRUE"
//        ]
//        def cells = excel.getCellsConditionally(columnMap,0)
//        cells.each {println it}
    }
}

