// ======================================================================
// Package Name    : DevelopFrameWork
// Class Name      : ReadGridExcel
// Creation Date   : 2021/08/03
// ======================================================================
package com.excel;

import lombok.Getter;
import lombok.Setter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.ss.usermodel.DateUtil;

/**
 * Excelの一覧表を読み込む。.xlsxに対応。
 * 
 * @author Yasuhiro Harada
 * @version 1.0.0
 */
@Getter
@Setter
public class ReadGridExcel {

    private XSSFWorkbook currentWorkBook = null;
    private XSSFSheet currentSheet = null;
    private XSSFRow currentRow = null;
    private int firstRowIndex;
    private int firstColumnIndex;
    private int currentRowIndex;
    private int currentColumnIndex;
    private int lastColumnIndex;
    private int lastRowIndex;

    /**
     * コンストラクタ
     * @param path 読み込むパス付ファイル名(xlsx)
     * @throws Exception_FileNotFoundException_IOException
     */
    public ReadGridExcel(String path) throws Exception, FileNotFoundException, IOException
    {
        
        if(!Files.exists(Paths.get(path))){
            throw new FileNotFoundException(path + "が存在しません。");
        }
        
        currentWorkBook = new XSSFWorkbook(new FileInputStream(path));
        if(currentWorkBook == null){
            throw new Exception(path + "はExcelﾌｧｲﾙ形式ではありません。");
        }
    }
    /**
     * すべてのSheet名を取得
     * @return すべてのSheet名
     */
    public List<String> GetAllSheeName()
    {
        List<String> sheetName = new ArrayList<>();

        for(int i = 0; i < currentWorkBook.getNumberOfSheets(); i++){
            sheetName.add(currentWorkBook.getSheetName(i));
        }
        return sheetName;
    }
    
    /**
     * 指定したSheetObjectを開く
     * @param sheetName 読み込むSheet名
     * @param firstRowIndex 読み込む最初の行(0スタート)
     * @param firstColumnIndex 読み込む最初の列(0スタート)
     * @throws Exception_FileNotFoundException_IOException
     */
    public void OpenSheet(String sheetName, int firstRowIndex, int firstColumnIndex) throws Exception
    {
        
        if(sheetName.isEmpty()){
            throw new Exception("OpenするSheet名を指定してください。");
        }
        if(firstRowIndex < 0){
            throw new Exception("firstRowIndexは0以上を設定してください。");
        }
        if(firstColumnIndex < 0){
            throw new Exception("firstColumnIndexは0以上を設定してください。");
        }
        
        XSSFSheet currentSheet = currentWorkBook.getSheet(sheetName);
        if(currentSheet == null){
            throw new Exception("「" + sheetName + "」Sheetは存在しません。");
        }
        
        this.currentSheet = currentSheet;
        this.firstRowIndex = firstRowIndex;
        this.firstColumnIndex = firstColumnIndex;
        this.currentRowIndex = firstRowIndex;
        this.currentColumnIndex = firstColumnIndex;

        lastRowIndex = currentSheet.getLastRowNum();

        currentRow = currentSheet.getRow(firstRowIndex);
        if(currentRow == null){
            throw new Exception("「" + sheetName + "」Sheetに1行もデータが存在しません。");
        }
        lastColumnIndex = currentRow.getLastCellNum() - 1;
    }

    /**
     * 次の行をフェッチする
     * @return true：正常、false：次の行がない
     */
    public Boolean moveNextRow()
    {
        Boolean ret = true;
        currentColumnIndex = firstColumnIndex;

        currentRow = currentSheet.getRow(++currentRowIndex);
        currentColumnIndex = firstColumnIndex;
        if(currentRow == null){
            if(lastRowIndex >= currentRowIndex){
                lastColumnIndex = firstColumnIndex - 1;
                return true;
            }
            else{
                return false;
            }
        }

        lastColumnIndex = currentRow.getLastCellNum() - 1;
        return ret;
    }

    /**
     * 次のセルの値を取得(行ごとに列数は異なる)
     * @return セルの値。null：次のセルがない
     */
    public String getNextCellValue()
    {
        String cellValue = "";

        // 行が取得できていなかった場合
        if(currentRow == null){
            if(lastColumnIndex >= currentColumnIndex){
                currentColumnIndex++;
            }
            else{
                cellValue = null;
            }
            return cellValue;
        }

        // セルの取得
        XSSFCell currentCell = currentRow.getCell(currentColumnIndex);
        if(currentCell == null){
            if(lastColumnIndex >= currentColumnIndex){
                currentColumnIndex++;
            }
            else{
                cellValue = null;
            }
            return cellValue;
        }

        // 値の取得
        switch(currentCell.getCellType()){
            
            case STRING:
                cellValue = currentCell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(currentCell)) {
                    Date date = currentCell.getDateCellValue();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    cellValue = dateFormat.format(date);
                } else {
                    cellValue = currentCell.getRawValue();
                }
                break;
            case FORMULA:
            case BOOLEAN:
            default:
                cellValue = currentCell.getRawValue();
                break;
        }

        currentColumnIndex++;
        return cellValue;
    }
 }