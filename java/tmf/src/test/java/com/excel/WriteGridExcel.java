// ======================================================================
// Package Name    : DevelopFrameWork
// Class Name      : WriteGridExcel
// Creation Date   : 2021/08/03
// ======================================================================
package com.excel;

import lombok.Getter;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Excelに一覧表を書き込む。.xlsxに対応。
 * 
 * @author Yasuhiro Harada
 * @version 1.0.0
 */
@Getter
@Setter
public class WriteGridExcel {

    private XSSFWorkbook currentWorkBook = null;
    private XSSFSheet currentSheet = null;
    private XSSFRow currentRow = null;
    private XSSFCell currentCell = null;
    private int curentRowIndex = 0;

    /**
     * コンストラクタ
     * @param sheetName 書き込むシート名
     * @throws Exception_FileNotFoundException_IOException
     */
    public WriteGridExcel(String sheetName) throws Exception, FileNotFoundException, IOException
    {
        if(sheetName.isEmpty()){
            throw new Exception("OpenするSheet名を指定してください。");
        }
        // ワークブックの作成
        currentWorkBook = new XSSFWorkbook();

        // シートの設定
        currentSheet = currentWorkBook.createSheet();
        currentWorkBook.setSheetName(0, sheetName);
        currentSheet = currentWorkBook.getSheet(sheetName);
    }
    
    /**
     * 行追加
     * @param colDatas 保存する1行分の列ごとの値
     * @throws Exception
     */
    public void AddRow(List<String> colDatas) throws Exception
    {
        currentRow = currentSheet.createRow(curentRowIndex);
        curentRowIndex++;
        for (int i = 0; i < colDatas.size(); i++) {
            currentCell = currentRow.createCell(i);
            currentCell.setCellValue(colDatas.get(i));
        }
    }

    /**
     * ファイルに保存
     * @param path 保存するpath付きファイル名
     * @throws Exception
     */
    public void Sava(String path) throws Exception
    {
        // ファイルが存在すれば削除する
        Files.deleteIfExists(Paths.get(path));

        // エクセルファイルを出力
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        currentWorkBook.write(fileOutputStream);
    }
 }