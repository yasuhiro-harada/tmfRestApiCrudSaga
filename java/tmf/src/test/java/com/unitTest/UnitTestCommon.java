package com.unitTest;

import oracle.ucp.jdbc.PoolDataSource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.example.tmf.DataSourceString;
import com.excel.ReadGridExcel;
import com.excel.WriteGridExcel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 単体テストのCommonクラス
 * 
 * @author Yasuhiro Harada
 * @version 1.0.0
 */
public class UnitTestCommon
{
    // application.propertiesの値を設定
    @Value("${rdbms.dataSourceName}") String dataSourceName;
    @Value("${rdbms.user}") String user;
    @Value("${rdbms.password}") String password;
    @Value("${spring.test.database.data.before-test}") String dataBeforeTest;

    // コネクションプール
    @Autowired
    private PoolDataSource poolDataSource;

    // データエクセルの読み取り開始行
    private final int TOPROW = 0;
    // データエクセルの読み取り開始列
    private final int LEFTCOLUMN = 0;
    // クラスのメンバーエクセルのシート名、単体テスト用クラスのインスタンス名
    private final String MEMBERSHEETNAME = "TestClass";
    // 列のデータ型
    // Keyの書式はテーブル名と列名を禁止文字の*で繋げたもの。例）mst_user*user_id
    // Valueはデータ型。例）char
    private Map<String, String> columnDataTypes = new HashMap<>();
    // 実行結果とOutExcelファイルの違いを表す列名
    private final String DIFFKUBUN = "DiffKubun";
    // Method名.Method名[Case番号]（メソッドごとにテストクラスがnewされる為、初期化される）
    private String errorCaseNo = "";
    // DB製品 
    private String databaseProduct = "";
    // DB名
    private String databaseName = "";
    /// 例外エラー発生テスト用フラグtrue:例外エラー発生。false:未発生。
    protected Boolean exceptionFlg = false;

    /**
     * 指定されたstackNum前のClass名とMethod名を取得
     * @param stackNum 何個前のStack情報から取り出すかを指定
     * @return <K, V>={<"ClassName", Class名>, <"MethodName", Method名>}
     */
    private Map<String, String> GetReturnInfo(int stackNum)
    {
        Map<String, String> returnInfo = new HashMap<>();
        StackTraceElement[] stackTraceElement = new Throwable().getStackTrace();
        //指定された前のスタックを取得
        //クラス名
        String[] returnInfoWork = stackTraceElement[stackNum].getClassName().split(Pattern.quote("."));
        returnInfo.put("ClassName", returnInfoWork[returnInfoWork.length - 1]);
        //メソッド名
        returnInfo.put("MethodName", stackTraceElement[stackNum].getMethodName()) ;
        return returnInfo;
    }
    
    /**
     * エラーケースNoを設定
     * @param className クラス名
     * @param methodName メソッド名
     * @param testCaseNo test case No
     */
    private void SetErrorCaseNo(String className, String methodName, int testCaseNo)
    {
        //Method名.Method名[Case番号]
        errorCaseNo = className + "." + methodName + "(TestCase:" + String.format("%03d", testCaseNo) + ")\r\n";
    }

    /**
     * TestDataExcelのpathを取得
     * @param io In：事前取り込み用TestData。Out：結果ｴﾋﾞﾃﾞﾝｽ確認用TestData。
     * @param type DB：Database。TestClass：Test対象ClassのField(Property)
     * @param className クラス名
     * @param methodName メソッド名
     * @param testCaseNo test case No
     * @return TestDataExcelのpath(file名付き)
     */
    private String GetExcelPath(String io, String type, String className, String methodName, int testCaseNo)
    {
        // classNameの末尾の"Test"を削除
        String classNameWork = className.substring(0, className.length() - 4);
        return "./src/test/UnitTestData/" + classNameWork + "/" + methodName +
                "/TestCase" + String.format("%03d", testCaseNo) + "/" + io + type + ".xlsx";
    }

    /**
     * 列名とデータ型を設定
     * @param resultSet ResultSet
     * @throws SQLException
     */
    private void AddColumnDataType(ResultSet resultSet) throws SQLException
    {
        // Keyの書式はテーブル名と列名を禁止文字の*で繋げたもの。例）mst_user*user_id
        String key = resultSet.getString("TABLE_NAME") + "*" + resultSet.getString("COLUMN_NAME");
        // Valueはデータ型。例）char
        String value = resultSet.getString("TYPE_NAME");
        columnDataTypes.put(key, value);
    }        

    /**
     * 列名とデータ型のセットを設定する
     * @param connection DBのコネクション
     * @param tableNames テーブル名リスト
     * @return 0：正常終了。1：正常終了。1部のテーブルは設定済だった。
     * @throws SQLException
     */
    private int AddColumnType(Connection connection, List<String> tableNames) throws SQLException
    {
        ResultSet resultSet = null;
        int ret = 0;

        try{

            Boolean matchFlg = false;

            for(String tableName : tableNames){

                // 該当テーブルがすでにdictionaryに追加されていたら
                for(String key : columnDataTypes.keySet()){
                    if(key.startsWith(tableName + "*")){
                        matchFlg = true;
                        ret = 1;
                        break;
                    }
                }
                if (matchFlg)
                {
                    matchFlg = false;
                    continue;
                }

                //データベースメタデータの取得
                DatabaseMetaData databaseMetaData = connection.getMetaData();

                //該当テーブルのすべての列名を取得
                resultSet = databaseMetaData.getColumns(databaseName, null, tableName, "%");

                // 取得結果をプロパティに設定
                while(resultSet.next())
                {
                    AddColumnDataType(resultSet);
                }
            }
        }catch(Exception ex){
            throw ex;
        }finally{
            if(resultSet != null){
                resultSet.close();
            }
        }
        return ret;
    }

    /**
     * テーブル定義（データなし）でコピーする
     * @param connection DBのコネクション
     * @param fromTableName コピー元テーブル名
     * @param toTableName コピー先テーブル名
     * @throws SQLException
     */
    private void CreateTable(Connection connection, String fromTableName, String toTableName) throws SQLException
    {

        PreparedStatement preparedStatement = null;

        try{
            String sql = "CREATE TABLE " + toTableName + " AS SELECT * FROM "+ fromTableName;
            switch(databaseProduct){
                case "oracle":
                    sql += " FETCH FIRST 0 ROWS ONLY ";
                    break;
                case "postgresql":
                    sql += " LIMIT 0 ";
                    break;
            }
            preparedStatement = connection.prepareStatement(sql);
            // SQL文を実行
            preparedStatement.executeUpdate();
        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * 対象のテーブルをDelete
     * @param connection DBのコネクション
     * @param tableName テーブル名
     * @throws SQLException
     */
    private void DeleteTable(Connection connection, String tableName) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "DELETE FROM " + tableName + ";";
            preparedStatement = connection.prepareStatement(sql);
            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * MERGE後にどちらのテーブルのレコードか判別する為の区分列を追加
     * @param connection DBのコネクション
     * @param tableName テーブル名
     * @throws SQLException
     */
    private void AddDiffKubunToTable(Connection connection, String tableName) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "ALTER TABLE " + tableName + " ADD " + DIFFKUBUN + " varchar(10) NOT NULL DEFAULT '';";
            preparedStatement = connection.prepareStatement(sql);
            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * 指定した２つのテーブルのデータが同じであることを確認
     * @param connection DBのコネクション
     * @param tableName テスト後の結果のテーブル名
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @param colNames Excelで指定された列名
     */
    private void RunMerge(Connection connection, String tableName, String createdTable, List<String> colNames) throws Exception
    {
        switch(databaseProduct){
            case "oracle":
                RunMergeOracle(connection, tableName, createdTable, colNames);
                break;
            case "postgresql":
                RunMergePostgres(connection, tableName, createdTable, colNames);
                break;
        }
    }

    /**
     * 指定した２つのテーブルのデータが同じであることを確認(Oracle用)
     * @param connection DBのコネクション
     * @param tableName テスト後の結果のテーブル名
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @param colNames Excelで指定された列名
     */
    private void RunMergeOracle(Connection connection, String tableName, String createdTable, List<String> colNames) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "";
            String colNameSql = "";
            String whereSql = "";
            sql = "MERGE INTO " + createdTable + " AS OutDB_EXCEL USING (" +
                "SELECT * FROM " + tableName + " ) AS DBAftTest ON (";
            for (String colName : colNames) {

                // 日付はJOIN条件に指定できない？
                switch(columnDataTypes.get(tableName + "*" + colName)){
                    case "timestamp":
                    case "date":
                    case "time":
                        break;
                    default:
                        whereSql += whereSql == "" ? " " : "AND ";
                        whereSql += "OutDB_EXCEL." + colName + " = DBAftTest." + colName + " ";
                        break;
                }
            }

            sql += whereSql;
            sql += ") WHEN MATCHED THEN DELETE WHEN NOT MATCHED BY TARGET THEN INSERT ( ";

            for (String colName : colNames) {
                colNameSql += colNameSql == "" ? " " : ", ";
                colNameSql += colName;
            }
            sql += colNameSql + ", " + DIFFKUBUN;
            sql += ") VALUES (";
            sql += colNameSql + ", '< DB'";
            sql += ") WHEN NOT MATCHED BY SOURCE THEN UPDATE SET " + DIFFKUBUN + " = '> OutExcel';";

            preparedStatement = connection.prepareStatement(sql);
            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * OutExcel全件のDIFFKUBUNに"> OutExcel"をUPDATE
     * @param connection DBのコネクション
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @throws SQLException
     */
    private void UpdateDiffkubunOutExcel(Connection connection, String createdTable) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "UPDATE " + createdTable + " SET " + DIFFKUBUN + " = '> OutExcel' ";
            preparedStatement = connection.prepareStatement(sql);

            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * OutExcelののDIFFKUBUNに"Delete"をDELETE
     * @param connection DBのコネクション
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @throws SQLException
     */
    private void DeleteDiffkubunDelete(Connection connection, String createdTable) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "DELETE FROM " + createdTable + " WHERE " + DIFFKUBUN + " = 'Delete' ";
            preparedStatement = connection.prepareStatement(sql);

            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * UnitTest後のDBテーブルとOutExcelの両方に存在する(一致し正解のレコード)のDIFFKUBUNに"Delete"をUPDATE
     * @param connection DBのコネクション
     * @param tableName テスト後の結果のテーブル名
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @param colNames Excelで指定された列名
     */
    private void UpdateDiffkubunDelete(Connection connection, String tableName, String createdTable, List<String> colNames) throws SQLException
    {
        PreparedStatement preparedStatement = null;

        try{
            String sql = "";
            String whereSql = "";

            sql = "UPDATE " + createdTable + " " +
                "SET " + DIFFKUBUN + " = 'Delete' FROM " + tableName + " ";

            for (String colName : colNames) {
                whereSql += whereSql == "" ? "WHERE " : "AND ";
                whereSql += createdTable + "." + colName + " = " + tableName + "." + colName + " ";
            }

            sql += whereSql;
            preparedStatement = connection.prepareStatement(sql);
            // SQL文を実行
            preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * UnitTest後のDBテーブルにしか存在しないレコードはDIFFKUBUNに"< DB"でINSERT
     * @param connection DBのコネクション
     * @param tableName テスト後の結果のテーブル名
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @param colNames Excelで指定された列名
     * @throws Exception
     */
    private void UpdateDiffkubunDB(Connection connection, String tableName, String createdTable, List<String> colNames) throws Exception 
    {
        PreparedStatement selectFromPreparedStatement = null;
        PreparedStatement selectToPreparedStatement = null;
        PreparedStatement insertPreparedStatement = null;
        ResultSet resultSetFrom = null;
        ResultSet resultSetTo = null;

        try{
            String selectFromSql = "";
            String selectToSql = "";
            String insertSql = "";
            String valueSql = "";
            String intoSql = "";
            String whereSQL = "";

            selectFromSql = "SELECT * FROM " + tableName + " ";
            selectFromPreparedStatement = connection.prepareStatement(selectFromSql);
            // SQL発行
            resultSetFrom = selectFromPreparedStatement.executeQuery();

            // 1レコードづつFetch
            while(resultSetFrom.next()){

                valueSql = "";
                intoSql = "";                
                selectToSql = "SELECT * FROM " + createdTable + " ";
                whereSQL = "";                
                insertSql = "INSERT INTO " + createdTable + "( ";

                // sql作成
                for (String colName : colNames) {
                    intoSql += intoSql == "" ? "" : ", ";
                    intoSql += colName;
                    valueSql += valueSql == "" ? " ?" : ",? ";
                    whereSQL += whereSQL == "" ? "WHERE " : "AND ";
                    whereSQL += colName + " = ? ";
                }
                intoSql += "," + DIFFKUBUN + " ";
                valueSql += ",? ";

                // Insert SQL prepare
                insertSql += intoSql + ")VALUES(" + valueSql + ")";
                insertPreparedStatement = connection.prepareStatement(insertSql);

                // Select SQL prepare
                selectToSql += whereSQL;
                selectToPreparedStatement = connection.prepareStatement(selectToSql);
    
                // placeholderを設定
                int i;
                for(i = 0; i < colNames.size(); i++){
                    switch(columnDataTypes.get(tableName + "*" + colNames.get(i))){
                        // FetchDataからDBのデータ型を取得
                        case "int4":
                        case "smallint":
                        case "integer":
                        case "bigint":
                        case "smallserial":
                        case "serial":
                        case "bigserial":
                        case "money":
                            insertPreparedStatement.setInt(i + 1, resultSetFrom.getInt(colNames.get(i)));
                            selectToPreparedStatement.setInt(i + 1, resultSetFrom.getInt(colNames.get(i)));
                            break;
                        case "character":
                        case "varying":
                        case "varchar":
                        case "char":
                        case "text":
                            insertPreparedStatement.setString(i + 1, resultSetFrom.getString(colNames.get(i)));
                            selectToPreparedStatement.setString(i + 1, resultSetFrom.getString(colNames.get(i)));
                            break;
                        case "decimal":
                        case "numeric":
                        case "real":
                        case "double precision":
                            insertPreparedStatement.setDouble(i + 1, resultSetFrom.getDouble(colNames.get(i)));
                            selectToPreparedStatement.setDouble(i + 1, resultSetFrom.getDouble(colNames.get(i)));
                            break;
                        case "timestamp":
                            insertPreparedStatement.setTimestamp(i + 1, resultSetFrom.getTimestamp(colNames.get(i)));
                            selectToPreparedStatement.setTimestamp(i + 1, resultSetFrom.getTimestamp(colNames.get(i)));
                            break;
                        case "date":
                            insertPreparedStatement.setDate(i + 1, resultSetFrom.getDate(colNames.get(i)));
                            selectToPreparedStatement.setDate(i + 1, resultSetFrom.getDate(colNames.get(i)));
                            break;
                        case "time":
                            insertPreparedStatement.setTime(i + 1, resultSetFrom.getTime(colNames.get(i)));
                            selectToPreparedStatement.setTime(i + 1, resultSetFrom.getTime(colNames.get(i)));
                            break;
                        default:
                            AssertFail(tableName + "テーブルの" + colNames.get(i) + "列はデータ型" + columnDataTypes.get(tableName + "*" + colNames.get(i)) + 
                                "です。本versionでは未対応のデータ型です。管理者に連絡してください。");
                    }
                }

                // 既存テーブルに同じデータのレコードがなければInsertする
                resultSetTo = selectToPreparedStatement.executeQuery();
                if(!resultSetTo.next()){
                    // SQL文を実行
                    insertPreparedStatement.setString(i + 1, "< DB");
                    insertPreparedStatement.executeUpdate();
                }
                resultSetTo.close();
                resultSetTo = null;
                selectToPreparedStatement.close();
                selectToPreparedStatement = null;
                insertPreparedStatement.close();
                insertPreparedStatement = null;
            }
        }finally{
            if(resultSetFrom  != null){
                resultSetFrom.close();
            }
            if(selectToPreparedStatement  != null){
                selectToPreparedStatement.close();
            }
            if(resultSetTo  != null){
                resultSetTo.close();
            }
            if(selectToPreparedStatement  != null){
                selectToPreparedStatement.close();
            }
            if(selectFromPreparedStatement  != null){
                selectFromPreparedStatement.close();
            }
            if(insertPreparedStatement  != null){
                insertPreparedStatement.close();
            }
        }
     }

    /**
     * 指定した２つのテーブルのデータが同じであることを確認(postgres用)
     * @param connection DBのコネクション
     * @param tableName テスト後の結果のテーブル名
     * @param createdTable OutDB.xlsxで指定されたデータが入っているテーブル名
     * @param colNames Excelで指定された列名
     * @throws SQLException
    */
    private void RunMergePostgres(Connection connection, String tableName, String createdTable, List<String> colNames) throws Exception
    {
        // OutExcel全件のDIFFKUBUNに"> OutExcel"をUPDATE
        UpdateDiffkubunOutExcel(connection, createdTable);

        // UnitTest後のDBテーブルとOutExcelの両方に存在する(一致し正解のレコード)のDIFFKUBUNに"Delete"をUPDATE
        UpdateDiffkubunDelete(connection, tableName, createdTable, colNames);

        // UnitTest後のDBテーブルにしか存在しないレコードはDIFFKUBUNに"< DB"でINSERT
        UpdateDiffkubunDB(connection, tableName, createdTable, colNames);

        // OutExcelののDIFFKUBUNに"Delete"をDELETE
        DeleteDiffkubunDelete(connection, createdTable);
    }

     /**
     * INSERT文を返す
     * @param tableName テーブル名
     * @param colNames 列名
     * @return INSERT文
     */
    private String MakeInsertSql(String tableName, List<String> colNames)
    {
        String sql = "";
        String columnsSql = "";
        String valueSql = "";
        Boolean commaFlg = false;

        sql = "INSERT INTO ";
        sql += tableName;
        sql += " (";

        for(String colName : colNames){
            if(commaFlg){
                columnsSql += ", ";
                valueSql += ", ";
            }
            commaFlg = true;
            columnsSql += colName;
            valueSql += "?";
        }

        sql += columnsSql;
        sql += ") VALUES (";
        sql += valueSql;
        sql += ")";

        return sql;
    }

    /**
     * INSERT文を実行
     * @param connection DB接続
     * @param sql sql文
     * @param cellValues INSERTする値
     * @param tableName テーブル名
     * @param colNames 列名
     * @return 更新した行数
     * @throws SQLException
     */
    private int excecInsert(Connection connection, String sql, List<String> cellValues, String tableName, List<String> colNames) throws Exception
    {

        PreparedStatement preparedStatement = null;
        int ret = 0;

        try{
            preparedStatement = connection.prepareStatement(sql);
            SimpleDateFormat simpleDateFormat;
            long miliSec;
            Boolean defaultFlg = false;
            int i;

            for(i = 0; i < cellValues.size(); i++){
                if(cellValues.get(i).toLowerCase().trim().equals("null")){
                    preparedStatement.setNull(i + 1, java.sql.Types.NULL);
                    continue;
                }
                // データ型を取得
                try{
                    switch(columnDataTypes.get(tableName + "*" + colNames.get(i))){
                        case "int4":
                        case "smallint":
                        case "integer":
                        case "bigint":
                        case "smallserial":
                        case "serial":
                        case "bigserial":
                        case "money":
                            preparedStatement.setInt(i + 1, Integer.parseInt(cellValues.get(i)));
                            break;
                        case "character":
                        case "varying":
                        case "varchar":
                        case "char":
                        case "text":
                            preparedStatement.setString(i + 1, cellValues.get(i));
                            break;
                        case "decimal":
                        case "numeric":
                        case "real":
                        case "double precision":
                            preparedStatement.setDouble(i + 1, Double.parseDouble(cellValues.get(i)));
                            break;
                        case "timestamp":
                            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            miliSec = simpleDateFormat.parse(cellValues.get(i)).getTime();
                            Timestamp timeStamp = new Timestamp(miliSec);
                            preparedStatement.setTimestamp(i + 1, timeStamp);
                            break;
                        case "date":
                            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            miliSec = simpleDateFormat.parse(cellValues.get(i)).getTime();
                            Date date = new Date(miliSec);
                            preparedStatement.setDate(i + 1, date);
                            break;
                        case "time":
                            simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                            miliSec = simpleDateFormat.parse(cellValues.get(i)).getTime();
                            Time time = new Time(miliSec);
                            preparedStatement.setTime(i + 1, time);
                            break;
                        default:
                            defaultFlg = true;
                    }
                    if(defaultFlg){
                        break;
                    }
                }catch(Exception ex){
                    AssertFail(
                        colNames.get(i) + "列はデータ型" + columnDataTypes.get(tableName + "*" + colNames.get(i)) +
                        "ですが、" + cellValues.get(i) + "が設定されています。InDB.xlsxの" + tableName + "シートに定義したTestDataを修正してください。");
                }
            }   
            if(defaultFlg){
                AssertFail("InDB.xlsxの" + tableName + "シートに定義した" + colNames.get(i) + "列のデータ型" +
                    columnDataTypes.get(tableName + "*" + colNames.get(i)) +
                    "は本versionでは未対応のデータ型です。管理者に連絡してください。");
            }

            // sqlの実行
            try{
                ret = preparedStatement.executeUpdate();
            }catch(Exception ex){
                AssertFail("[SQL Error]sql=" + sql +", values=" + cellValues + "。InDB.xlsxの" + tableName + "シートに設定したTestDataを修正してください。");
            }
        }catch(Exception ex){
            throw ex;
        }finally{
            if(preparedStatement != null){
                preparedStatement.close();
            }
        }
        return ret;
    }

    /**
     * Exception をスローします
     * @param errorMsg エラーメッセージ
     * @throws Exception
     */
    protected void AssertFail(String errorMsg) throws Exception
    {
        throw new Exception(errorCaseNo + errorMsg);
    }

    /**
     * Exception をスローします
     * @throws Exception
     */
    protected void AssertFail() throws Exception
    {
        AssertFail("");
    }

    /**
     * 指定した値が等しくない場合は例外をスローします。
     * @param <T>
     * @param expected 比較する最初の値。これはテストで予期される値です。
     * @param actual 比較する 2 番目の値。これはテストのコードで生成される値です。
     * @param errorMsg expectedとactualが等しくない場合にテスト結果に表示されるメッセージ
     * @throws Exception
     */
    protected <T> void AreEqual(T expected, T actual, String errorMsg) throws Exception
    {

        // 値が同じであれば何もせず終了
        // 値がnullの場合
        if(expected == null || actual == null){
            if(expected == null && actual == null){
                return;
            }
        }
        // 値が文字列の場合
        if(expected instanceof String){
            if(expected.equals(actual)){
                return;
            }
        }
        // 値がその他の場合
        else if(expected == actual){
            return;
        }

        // 値が異なればAssert
        AssertFail(errorMsg);
    }

    /**
     * 指定した値が等しい場合は例外をスローします。
     * @param <T>
     * @param expected 比較する最初の値。これはテストで予期される値です。
     * @param actual 比較する 2 番目の値。これはテストのコードで生成される値です。
     * @param errorMsg expectedとactualが等しい場合にテスト結果に表示されるメッセージ
     * @throws Exception
     */
    protected <T> void AreNotEqual(T expected, T actual, String errorMsg) throws Exception
    {

        // 値が異なれば何もせず終了
        // 値がnullの場合
        if(expected == null || actual == null){
            if(expected != null || actual != null){
                return;
            }
        }
        // 値が文字列の場合
        else if(expected instanceof String){
            if(!expected.equals(actual)){
                return;
            }
        }
        // 値がその他の場合
        else if(expected != actual){
            return;
        }

        // 値が異なればAssert
        AssertFail(errorMsg);
    }

    /**
     * Inser文を作成し返す
     * @param readGridExcel DB Excelﾌｧｲﾙ
     * @param tableName Excelシート名のテーブル名
     * @param createdTable Excelシート名から作成した一時表のテーブル名
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @param colNames 列名のリスト
     * @return Insert SQL 文
     * @throws Exception
     */
    private String GetInsertSql(ReadGridExcel readGridExcel, String tableName, String createdTable, String io, List<String> colNames) throws Exception
    {
        String sql = "";

        // 1行目は列名（前提条件）のため一列目を先に読み取る。
        // 異なる場合は考慮しない
        try{
            readGridExcel.OpenSheet(tableName, TOPROW, LEFTCOLUMN);
        }catch(Exception ex){
            AssertFail(ex.getMessage());
        }
        String cellValue  = readGridExcel.getNextCellValue();
        AreNotEqual(cellValue, "",
            io + "DB.xlsxの読み取りに失敗しました。1行目はDBの列名にしてください。");
        
        // 1行目を左から順に空白まで読み込み列名に設定する
        while(cellValue != null && !cellValue.isEmpty()){
            colNames.add(cellValue);
            cellValue  = readGridExcel.getNextCellValue();
        }

        // EXCELの列名チェック
        for(String colName : colNames)
        {
            AreNotEqual(columnDataTypes.get(tableName + "*" + colName), null,
                io + "DB.xlsxのシート名(" + tableName + ")または列名(" + colName + ")が不正です。");
        }

        // Insert用のSQL文を作成する
        if(io.equals("In")){
            sql = MakeInsertSql(tableName, colNames);
        }
        else if(io.equals("Out")){
            sql = MakeInsertSql(createdTable, colNames);
        }
        return sql;
    }

    /**
     * RecordをInsert
     * @param readGridExcel DB Excelﾌｧｲﾙ
     * @param connection DBコネクション
     * @param sql Insertのsql
     * @param tableName Excelシート名のテーブル名
     * @param createdTable Excelシート名から作成した一時表のテーブル名
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @param colNames 列名のリスト
     * @throws Exception
     */
    private void execInsertSql(ReadGridExcel readGridExcel, Connection connection, String sql, String tableName, String createdTable, String io, List<String> colNames) throws Exception
    {
        // RecordをInsertしていく。
        while(readGridExcel.moveNextRow()){

            List<String> cellValues = new ArrayList<>();
            Boolean existFlg = false;

            String cellValue = readGridExcel.getNextCellValue();

            // 列分以下を繰り返し、Insertする値を取得
            while(cellValue != null){
                cellValues.add(cellValue);
                if(!cellValue.isBlank()){
                    existFlg = true;
                }
                cellValue = readGridExcel.getNextCellValue();
            }
            // 全てのセルが空文字だったら、insert文を発行せずに次の行へ行く
            if(existFlg == false){
                break;
            }

            // INSERT文発行
            excecInsert(connection, sql, cellValues, tableName, colNames);
        }
    }

    /**
     * 
     * @param connection
     * @param className
     * @param methodName
     * @param tableName
     * @param createdTable
     * @param colNames
     * @param testCaseNo
     * @throws Exception
     */
    private void OutputMergeResult(Connection connection, String className, String methodName, String tableName, String createdTable, List<String> colNames, int testCaseNo) throws Exception
    {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean writeFlg = false;

        try{

            WriteGridExcel writeGridExcel = new WriteGridExcel(tableName);
            List<String> colDatas = new ArrayList<>();

            // 列名出力
            colDatas.add(DIFFKUBUN);
            for (String colName : colNames) {
                colDatas.add(colName);
            }
            writeGridExcel.AddRow(colDatas);

            // sql作成
            String sql = "SELECT " + DIFFKUBUN;
            String colSql = "";
            for (String colName : colNames) {
                colSql += colSql == "" ? "" : " ,";
                colSql += colName;
            }            
            sql += ", " + colSql + " FROM " + createdTable + " ORDER BY " + colSql;

            preparedStatement = connection.prepareStatement(sql);
            // SQL発行
            resultSet = preparedStatement.executeQuery();
            // 1レコードづつFetch
            while(resultSet.next()){

                colDatas.clear();
                // Diff Kubun 追加
                colDatas.add(resultSet.getString(DIFFKUBUN));

                int i;
                for(i = 0; i < colNames.size(); i++){
                    switch(columnDataTypes.get(tableName + "*" + colNames.get(i))){
                        case "int4":
                        case "smallint":
                        case "integer":
                        case "bigint":
                        case "smallserial":
                        case "serial":
                        case "bigserial":
                        case "money":
                            colDatas.add(Integer.valueOf(resultSet.getInt(colNames.get(i))).toString());
                            break;
                        case "character":
                        case "varying":
                        case "varchar":
                        case "char":
                        case "text":
                            colDatas.add(resultSet.getString(colNames.get(i)));
                            break;
                        case "decimal":
                        case "numeric":
                        case "real":
                        case "double precision":
                            colDatas.add(Double.valueOf(resultSet.getDouble(colNames.get(i))).toString());
                            break;
                        case "timestamp":
                            colDatas.add(resultSet.getTimestamp(colNames.get(i)).toString());
                            break;
                        case "date":
                            colDatas.add(resultSet.getDate(colNames.get(i)).toString());
                            break;
                        case "time":
                            colDatas.add(resultSet.getTime(colNames.get(i)).toString());
                            break;
                        default:
                            AssertFail(tableName + "テーブルの" + colNames.get(i) + "列はデータ型" + columnDataTypes.get(tableName + "*" + colNames.get(i)) + 
                                "です。本versionでは未対応のデータ型です。管理者に連絡してください。");
                    }
                }
                // 行の書き込み
                writeGridExcel.AddRow(colDatas);
                writeFlg = true;
            }
            if(writeFlg){
                // Excelの上書き保存
                String path = GetExcelPath("Chk", "DB", className, methodName, testCaseNo);
                writeGridExcel.Sava(path);
                AssertFail("OutDBの結果と一致しません。" + path + "を確認して下さい。");
            }
        }finally{
            if(resultSet  != null){
                resultSet.close();
            }
            if(preparedStatement  != null){
                preparedStatement.close();
            }
        }
    }

    /**
     * テストデータ投入 or テストデータをマッチング
     * @param connection DB接続
     * @param className クラス名
     * @param methodName メソッド名
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @param testCaseNo TestCaseNo
     * @param databaseFlg DB利用フラグ。true=利用する。false=利用しない。
     */
    private void DBTestData(Connection connection, String className, String methodName, String io, int testCaseNo, Boolean databaseFlg) throws Exception, FileNotFoundException, IOException
    {
        if(!databaseFlg){
            return;
        }

        // InDB.xlsxの列定義のデータタイプを取得
        String path = GetExcelPath(io, "DB", className, methodName, testCaseNo);
        if(!Files.exists(Paths.get(path)))
        {
            return;
        }

        ReadGridExcel readGridExcel = null;
        try{
            readGridExcel = new ReadGridExcel(path);
        }catch(Exception ex){
            AssertFail(ex.getMessage());
        }
        List<String> tableNames = readGridExcel.GetAllSheeName();
        AddColumnType(connection, tableNames);
        // boolean tableCreatedFlg = false;
        String createdTable = "";

        // シート数分以下を繰り返す
        for(String tableName : tableNames){

            // tableCreatedFlg = false;

            if(io.equals("In")){
                // 該当テーブルを削除。テスト前のデータを消して良ければ削除。
                // (test対象とconnectionのトランザクションが共有できないUnitTestやtest対象が別システムでDBを更新する場合など)
                DeleteTable(connection, tableName);
                if(dataBeforeTest.equals("delete")){
                    connection.commit();
                }
            }
            else if(io.equals("Out")){
                // 想定する結果の一時テーブル名を作成
                LocalDateTime localDataTime = LocalDateTime.now();
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
                createdTable = tableName + dateTimeFormatter.format(localDataTime);
                // テーブル定義（データなし）でコピーする
                CreateTable(connection, tableName, createdTable);
                // tableCreatedFlg = true;
                // MERGE後にどちらのテーブルのレコードか判別する為の区分列を追加
                AddDiffKubunToTable(connection, createdTable);
            }

            List<String> colNames = new ArrayList<>();
            // Insert用のSQL文を作成する
            String  sql = GetInsertSql(readGridExcel, tableName, createdTable, io, colNames);

            // RecordをInsert
            execInsertSql(readGridExcel, connection, sql, tableName, createdTable, io, colNames);

            if(io.equals("Out")){
                // MERGE文の発行
                RunMerge(connection, tableName, createdTable, colNames);
                // MERGE結果を確認
                OutputMergeResult(connection, className, methodName, tableName, createdTable, colNames, testCaseNo);
            }
        }
    }

    /**
     * OutTestClass.xlsxのField名と配列Indexを分割
     * @param memberName Field名[index]
     * @param aryMember 出力引数。aryMember[0] : メンバー名。aryMember[1]：配列の場合はIndex。index未指定の場合は"-1"。
     * @return index。-1はindex未指定。
     */
    private int GetArrayData(String memberName, String[] aryMember) throws Exception
    {
        int aryIndex = -1;
        String[] aryWork = memberName.split(Pattern.quote("["));
        aryMember[0] = aryWork[0];
        aryMember[1] = "-1";

        // 配列の場合はIndexを取得
        if(aryWork.length > 1)
        {
            aryWork[1] = aryWork[1].substring(0, aryWork[1].length() - 1);
            try{
                aryIndex = Integer.parseInt(aryWork[1]);
                if(aryIndex < 0){
                    throw new Exception();
                }
            }
            catch(Exception ex){
                AssertFail("InTestClass.xlsxの" + MEMBERSHEETNAME + "シートの" + aryWork[0] + "で指定されている配列の添え字が不正な形式です。");
            }
            aryMember[1] = aryWork[1];
        }
        return aryIndex;
    }

    /**
     * メンバ名のピリオドの数だけ再帰的に呼ぶ
     * @param testClass RootのObjectクラス
     * @param memberNames メンバー名
     * @param value 設定する値
     * @param aryMember aryMember[0] : メンバー名、aryMember[1]：配列の場合はIndex
     * @param aryIndex 配列のIndex
     * @param field メンバー名で指定されたFieldオブジェクト
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @return 0:最深層、1:途中の階層
     * @throws Exception
     */
    private int NestMemberData(Object testClass, List<String> memberNames, String value, String[] aryMember, int aryIndex, Field field, String io) throws Exception
    {
        // メンバ名にピリオドが無ければ処理しない
        if (memberNames.size() == 1){
            // @return 0:最深層、1:途中の階層
            return 0;
        }

        Object nestTestClass = null;
            
        // 配列の場合、添え字を使って値を取り出す
        if(field.getType().isArray()){

            Object[] objFields = (Object[])field.get(testClass);
            if(aryIndex < 0){
                AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "は配列ですが、配列として指定されていません。" + io + "TestClass.xlsxの" +
                    MEMBERSHEETNAME + "シートのTestDataを修正してください。");
            }
            else if(aryIndex > objFields.length - 1){
                int aryCnt = objFields.length;
                AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "はsize=" + Integer.toString(aryCnt) + "ですが、添え字として" +
                    Integer.toString(aryIndex) + "が指定されています。" + io + "TestClass.xlsxの" + MEMBERSHEETNAME + "シートのTestDataを修正してください。");
            }
            
            nestTestClass = Array.get(objFields, aryIndex);
        }
        // Listの場合、添え字を使って値を取り出す
        else if(field.getType().getName().equals("java.util.List")){
            List<?> objFields = (List<?>)field.get(testClass);
            int aryCnt = objFields.size();
            if(aryIndex < 0){
                AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "はListですが、配列として指定されていません。" + io + "TestClass.xlsxの" +
                    MEMBERSHEETNAME + "シートのTestDataを修正してください。");
            }
            else if(aryIndex > aryCnt - 1){
                // if(io.equals("Out")){
                AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "はsize=" + Integer.toString(aryCnt) + "ですが、添え字として" +
                Integer.toString(aryIndex) + "が指定されています。" + io + "TestClass.xlsxの" + MEMBERSHEETNAME + "シートのTestDataを修正してください。");
                // }else if(io.equals("In")){
                //     // List<?>の?のクラス名を取得
                //     String type = field.getAnnotatedType().getType().getTypeName();
                //     String[] types = type.split("<");
                //     types = types[1].split(">");
                //     type = types[0];
                //     while(aryIndex >= objFields.size()){
                //         Class<?> genericClass = Class.forName(type);
                //         objFields.add(genericClass);
                //         System.out.println("x");
                //     }

                // }
            }
            nestTestClass = objFields.get(aryIndex);
        }
        else{
            nestTestClass = field.get(testClass);
        }
        
        if(nestTestClass == null){
            AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "はnullです。" + 
                io + "TestClass.xlsxの" + MEMBERSHEETNAME + "シートのTestDataまたはプログラムを修正してください。");
        }
        //ピリオド１個分削除し、１つ深い階層でCheckする
        memberNames.remove(0);
        // ピリオドがなくなるまで再帰的にSetMemberDataを呼ぶ
        MemberData(nestTestClass, memberNames, value, io);

        // @return 0:最深層、1:途中の階層
        return 1;
    }

    /**
     * Class Memberに値を設定。メンバー名のピリオドの文字数だけ再帰的に呼ぶ
     * @param testClass RootのObjectクラス
     * @param memberNames メンバー名
     * @param value 設定する値
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @throws Exception
     */
    private void MemberData(Object testClass, List<String> memberNames, String value, String io) throws Exception
    {

        // aryMember[0] : メンバー名、aryMember[1]：配列の場合はIndex
        String[] aryMember = new String[2];

        // メンバー名を取得（配列の場合はIndexも）同一メンバー名でない場合は次のメンバーを探す
        int aryIndex = GetArrayData(memberNames.get(0), aryMember);

        Field field = null;
        try{
            field = testClass.getClass().getDeclaredField(aryMember[0]);
        }catch(Exception ex){
            AssertFail(testClass.getClass().getName() + "には" + aryMember[0] + "Fieldが存在しません。" + io + "TestClass.xlsxの" +
                MEMBERSHEETNAME + "シートのTestDataを修正してください。");
        }

        // private fieldでも値を取得可能
        field.setAccessible(true);
        
        // Excelで配列指定の場合は、その配列のIndex番目に入れ替える
        if(NestMemberData(testClass, memberNames, value, aryMember, aryIndex, field, io) == 1){
            return;
        }

        // Fieldに値を設定
        if(io.equals("In")){
            SetFieldValue(testClass, value, aryMember, aryIndex, field);
        }
        else if(io.equals("Out")){
            CompareFieldValue(testClass, value, aryMember, aryIndex, field);
        }
    }

    /**
     * Fieldに値を設定
     * @param testClass RootのObjectクラス
     * @param value 設定する値
     * @param aryMember aryMember[0] : メンバー名、aryMember[1]：配列の場合はIndex
     * @param aryIndex 配列のIndex
     * @param field メンバー名で指定されたFieldオブジェクト
     * @throws Exception
     */
    private void SetFieldValue(Object testClass, String value, String[] aryMember, int aryIndex, Field field) throws Exception
    {
        boolean defaultFlg = false;
        boolean blnWork = false;

        try{
            switch(field.getGenericType().getTypeName()){
                case "java.lang.String":
                    field.set(testClass, value);
                    break;
                case "java.lang.Integer":
                    field.set(testClass, Integer.valueOf(value));
                    break;
                case "int":
                    field.setInt(testClass, Integer.parseInt(value));
                    break;
                case "java.lang.Float":
                    field.set(testClass, Float.valueOf(value));
                    break;
                case "float":
                    field.setFloat(testClass, Float.parseFloat(value));
                    break;
                case "java.lang.Boolean":
                    blnWork = ChangeBooleanExcelToJava(value);
                    field.set(testClass, Boolean.valueOf(blnWork));
                    break;
                case "boolean":
                    blnWork = ChangeBooleanExcelToJava(value);
                    field.setBoolean(testClass, blnWork);
                    break;
                case "java.lang.Double":
                    field.set(testClass, Double.valueOf(value));
                    break;
                case "double":
                    field.setDouble(testClass, Double.parseDouble(value));
                    break;
                case "java.lang.Long":
                    field.set(testClass, Long.valueOf(value));
                    break;
                case "long":
                    field.setLong(testClass, Long.parseLong(value));
                    break;
                case "java.lang.Byte":
                    field.set(testClass, Byte.valueOf(value));
                    break;
                case "byte":
                    field.setByte(testClass, Byte.parseByte(value));
                    break;
                case "java.lang.Short":
                    field.set(testClass, Short.valueOf(value));
                    break;
                case "short":
                    field.setShort(testClass, Short.parseShort(value));
                    break;
                case "java.lang.Character":
                    field.set(testClass, Character.valueOf(ChangeCharExcelToJava(value)));
                    break;
                case "char":
                    field.setChar(testClass, ChangeCharExcelToJava(value));
                    break;
                case "java.lang.String[]":
                    Array.set(field.get(testClass), aryIndex, value);
                    break;
                case "java.lang.Integer[]":
                    Array.set(field.get(testClass), aryIndex, Integer.valueOf(value));
                    break;
                case "int[]":
                    Array.setInt(field.get(testClass), aryIndex, Integer.parseInt(value));
                    break;
                case "java.lang.Float[]":
                    Array.set(field.get(testClass), aryIndex, Float.valueOf(value));
                    break;
                case "float[]":
                    Array.setFloat(field.get(testClass), aryIndex, Float.parseFloat(value));
                    break;
                case "java.lang.Boolean[]":
                    blnWork = ChangeBooleanExcelToJava(value);
                    Array.set(field.get(testClass), aryIndex, Boolean.valueOf(blnWork));
                    break;
                case "boolean[]":
                    blnWork = ChangeBooleanExcelToJava(value);
                    Array.setBoolean(field.get(testClass), aryIndex, blnWork);
                    break;
                case "java.lang.Double[]":
                    Array.set(field.get(testClass), aryIndex, Double.valueOf(value));
                    break;
                case "double[]":
                    Array.setDouble(field.get(testClass), aryIndex, Double.parseDouble(value));
                    break;
                case "java.lang.Long[]":
                    Array.set(field.get(testClass), aryIndex, Long.valueOf(value));
                    break;
                case "long[]":
                    Array.setLong(field.get(testClass), aryIndex, Long.parseLong(value));
                    break;
                case "java.lang.Byte[]":
                    Array.set(field.get(testClass), aryIndex, Byte.valueOf(value));
                    break;
                case "byte[]":
                    Array.setByte(field.get(testClass), aryIndex, Byte.parseByte(value));
                    break;
                case "java.lang.Short[]":
                    Array.set(field.get(testClass), aryIndex, Short.valueOf(value));
                    break;
                case "short[]":
                    Array.setShort(field.get(testClass), aryIndex, Short.parseShort(value));
                    break;
                case "java.lang.Character[]":
                    Array.set(field.get(testClass), aryIndex, Character.valueOf(ChangeCharExcelToJava(value)));
                    break;
                case "char[]":
                    Array.setChar(field.get(testClass), aryIndex, ChangeCharExcelToJava(value));
                    break;
                default:
                    defaultFlg = true;
            }
        }catch(Exception ex){
            AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "は" +
                field.getGenericType().getTypeName() + "型ですが、\"" + value + "\"が指定されています。InTestClass.xlsxの" +
                MEMBERSHEETNAME + "シートのTestDataを修正してください。");
        }

        if(defaultFlg){
            AssertFail(testClass.getClass().getName() + "の" + aryMember[0] +
            "は" + testClass.getClass().getTypeName() + "型ですが、" +
            "この型は本versionでは未対応のデータ型です。管理者に連絡してください。");
        }
    }

    /**
     * Fieldの値とExcelの値を比較
     * @param testClass RootのObjectクラス
     * @param value 設定する値
     * @param aryMember aryMember[0] : メンバー名、aryMember[1]：配列の場合はIndex
     * @param aryIndex 配列のIndex
     * @param field メンバー名で指定されたFieldオブジェクト
     * @throws Exception
     */
    private void CompareFieldValue(Object testClass, String value, String[] aryMember, int aryIndex, Field field) throws Exception
    {
        boolean defaultFlg = false;
        boolean diffFlg = false;
        boolean blnWork = false;
        boolean booleanFlg = false;

        try{
            switch(field.getGenericType().getTypeName()){
                case "java.lang.String":
                    if(!value.equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Integer":
                    if(!Integer.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "int":
                    if(Integer.parseInt(value) != field.getInt(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Float":
                    if(!Float.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "float":
                    if(Float.parseFloat(value) != field.getFloat(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Boolean":
                    booleanFlg = true;
                    blnWork = ChangeBooleanExcelToJava(value);
                    if(!Boolean.valueOf(blnWork).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "boolean":
                    booleanFlg = true;
                    blnWork = ChangeBooleanExcelToJava(value);
                    if(blnWork != field.getBoolean(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Double":
                    if(!Double.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "double":
                    if(Double.parseDouble(value) != field.getDouble(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Long":
                    if(!Long.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "long":
                    if(Long.parseLong(value) != field.getLong(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Byte":
                    if(!Byte.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "byte":
                    if(Byte.parseByte(value) != field.getByte(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Short":
                    if(!Short.valueOf(value).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "short":
                    if(Short.parseShort(value) != field.getShort(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Character":
                    if(!Character.valueOf(ChangeCharExcelToJava(value)).equals(field.get(testClass))){
                        diffFlg = true;
                    }
                    break;
                case "char":
                    if(ChangeCharExcelToJava(value) != field.getChar(testClass)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.String[]":
                    if(!value.equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Integer[]":
                    if(!Integer.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "int[]":
                    if(Integer.parseInt(value) != Array.getInt(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Float[]":
                    if(!Float.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "float[]":
                    if(Float.parseFloat(value) != Array.getFloat(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Boolean[]":
                    booleanFlg = true;
                    blnWork = ChangeBooleanExcelToJava(value);
                    if(!Boolean.valueOf(blnWork).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "boolean[]":
                    booleanFlg = true;
                    blnWork = ChangeBooleanExcelToJava(value);
                    if(blnWork != Array.getBoolean(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Double[]":
                    if(!Double.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "double[]":
                    if(Double.parseDouble(value) != Array.getDouble(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Long[]":
                    if(!Long.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "long[]":
                    if(Long.parseLong(value) != Array.getLong(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Byte[]":
                    if(!Byte.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "byte[]":
                    if(Byte.parseByte(value) != Array.getByte(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Short[]":
                    if(!Short.valueOf(value).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "short[]":
                    if(Short.parseShort(value) != Array.getShort(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                case "java.lang.Character[]":
                    if(!Character.valueOf(ChangeCharExcelToJava(value)).equals(Array.get(field.get(testClass), aryIndex))){
                        diffFlg = true;
                    }
                    break;
                case "char[]":
                    if(ChangeCharExcelToJava(value) != Array.getChar(field.get(testClass), aryIndex)){
                        diffFlg = true;
                    }
                    break;
                default:
                    defaultFlg = true;
            }
        }catch(Exception ex){
            AssertFail(testClass.getClass().getName() + "の" + aryMember[0] + "は" +
                field.getGenericType().getTypeName() + "型ですが、\"" + value + "\"が指定されています。OutTestClass.xlsxの" +
                MEMBERSHEETNAME + "シートのTestDataを修正してください。");
        }

        if(diffFlg){
            String resultValue = "";
            String argName = "";
            if(aryIndex < 0){
                resultValue = field.get(testClass).toString();
                argName = aryMember[0];
            }else{
                resultValue = Array.get(field.get(testClass), aryIndex).toString();
                argName = aryMember[0] + "[" + aryMember[1] + "]";
            }
            if(booleanFlg){
                if(value == "1"){
                    value = "false";
                }else{
                    value = "true";
                }
            }
            AssertFail(testClass.getClass().getName() + "のメンバー変数「" + argName + "」にプログラムの実行結果として\"" +
                resultValue + "\"が設定されていますが、OutTestClass.xlsxには\"" + value + "\"が指定されています。" +
                MEMBERSHEETNAME + "シートのTestDataかプログラムを確認してください。");
        }
        else if(defaultFlg){
            AssertFail(testClass.getClass().getName() + "の" + aryMember[0] +
            "は" + testClass.getClass().getTypeName() + "型ですが、" +
            "この型は本versionでは未対応のデータ型です。管理者に連絡してください。");
        }
    }

    /**
     * Excedlで設定されているtrue/falseをJava形式に変換
     * @param value
     * @return 変換後の true or false
     * @throws Exception
     */
    private boolean ChangeBooleanExcelToJava(String value) throws Exception
    {
        boolean ret = false;
        value = value.toLowerCase();
        if(value.equals("1") || value.equals("true")){
            ret = true;
        }
        else if(value.equals("0") || value.equals("false")){
            ret = false;
        }
        else{
            AssertFail();
        }
        return ret;
    }

    /**
     * Excedlで設定されているcharをJava形式に変換
     * @param value
     * @return 変換後の char
     * @throws Exception
     */
    private char ChangeCharExcelToJava(String value) throws Exception
    {
        if(value.length() > 1){
            AssertFail();
        }
        else if(value.length() == 0){
            value = " ";
        }
        return value.charAt(0);
    }

    /**
     * メンバーフィールド、プロパティに値を設定する
     * @param memberName メーンバーField名
     * @param value 設定するテストデータ
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @return
     * @throws Exception
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    private void MemberDataEntryPoint(String memberName, String value, String io) throws Exception, IllegalArgumentException, NoSuchFieldException, SecurityException
    {
        // this（継承先）のTestClassを取得
        Object testClass = this;
        Field field = testClass.getClass().getDeclaredField(MEMBERSHEETNAME);
        // private fieldでも値を設定可能
        field.setAccessible(true);
        testClass = field.get(testClass);

        // Field名をピリオドで分割
        List<String> memberNames = new ArrayList<>(List.of(memberName.split(Pattern.quote("."))));

        MemberData(testClass, memberNames, value, io);
    }

    /**
     * クラスのメンバーへ更新前テストデータを挿入 or マッチング
     * @param className TestClass名
     * @param methodName TestMethod名
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @param testCaseNo test case No
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     */
    private void ClassMemberData(String className, String methodName, String io, int testCaseNo) throws FileNotFoundException, IOException, Exception
    {
        // InMember.xlsxの列定義のデータタイプを取得
        String path = GetExcelPath(io, "TestClass", className, methodName, testCaseNo);
        if(!Files.exists(Paths.get(path)))
        {
            return;
        }

        // メンバーフィールド、プロパティを取得
        ReadGridExcel readGridExcel = null;
        try{
            readGridExcel = new ReadGridExcel(path);
            readGridExcel.OpenSheet(MEMBERSHEETNAME, TOPROW, LEFTCOLUMN);
        }catch(Exception ex){
            AssertFail(ex.getMessage());
        }

        // 上から順に、メンバーフィールド、プロパティを読んでいく
        do{
            // FieldNameを取得
            String fieldName = readGridExcel.getNextCellValue();
            if(fieldName == null || fieldName.equals("")){
                break;
            }
            // 値を取得
            String fieldValue = readGridExcel.getNextCellValue();
            // メンバーフィールド、プロパティに値を設定する
            MemberDataEntryPoint(fieldName, fieldValue, io);
        }while (readGridExcel.moveNextRow());
    }

    /**
     * テストデータをDBとクラスのメンバーに投入する
     * @param connection DBのコネクション
     * @param className TestClass名
     * @param methodName TestMethod名
     * @param testCaseNo TestCaseNo
     * @param databaseFlg DB利用フラグ。true=利用する。false=利用しない。
     * @param io "In":テストデータ投入。"Out":テストデータをマッチング。
     * @throws Exception
     */ 
    private void TestData(Connection connection, String className, String methodName, int testCaseNo, Boolean databaseFlg, String io) throws Exception
    {

        // DBへ更新前テストデータを挿入
        DBTestData(connection, className, methodName, io, testCaseNo, databaseFlg);

        // クラスのメンバーへ更新前テストデータを挿入
        ClassMemberData(className, methodName, io, testCaseNo);
    }

    /**
     * Unit Test の前処理
     * @param databaseFlg DB利用フラグ。true=利用する。false=利用しない。
     * @throws Exception
     */
    protected void InitTest(Boolean databaseFlg) throws Exception
    {

        Connection connection = null;

        try{

            String testCaseMethodName = "";
            int ret = 0;
            Map<String, String> returnInfo = new HashMap<>();

            // 呼び出し元のクラス名とメソッド名を取得
            returnInfo = GetReturnInfo(2);

            // テストケース記述メソッド名設定
            testCaseMethodName = returnInfo.get("MethodName") + "TestCase";
            
            if(databaseFlg){
                // DB接続
                DataSourceString dataSourceString = new DataSourceString();
                try{
                    connection = dataSourceString.connectDB(poolDataSource);
                }
                catch(Exception ex){
                    throw new Exception("DBの接続に失敗しました。application.propertiesを修正して下さい。" + ex.getMessage());
                }

                // データベース情報を取得
                databaseProduct = dataSourceString.GetDatabaseProduct(dataSourceName);
                databaseName = dataSourceString.GetDatabaseName(dataSourceName);

                // オートコミットオフ
                connection.setAutoCommit(false);
            }

            for(int testCaseNo = 1; testCaseNo < 1000; testCaseNo++){

                // 例外エラー用フラグの初期化
                exceptionFlg = false;

                // エラーケースNo（Method名.Method名[Case番号]）
                SetErrorCaseNo(returnInfo.get("ClassName"), returnInfo.get("MethodName"), testCaseNo);
        
                // テストデータをDBとクラスのメンバーに投入する
                TestData(connection, returnInfo.get("ClassName"), returnInfo.get("MethodName"), testCaseNo, databaseFlg, "In");

                // テストケースメソッド取得
                Method testMethod = this.getClass().getDeclaredMethod(testCaseMethodName, Connection.class, int.class);
                // テストケースメソッドコール
                testMethod.setAccessible(true);
                ret = (int)testMethod.invoke(this, connection, testCaseNo);

                // テスト対象が更新した結果もテスト後の比較に含めるために再接続
                // (test対象とconnectionのトランザクションが共有できないUnitTestやtest対象が別システムでDBを更新する場合など)
                if(databaseFlg && dataBeforeTest.equals("delete")){
                    connection.commit();
                    connection.close();;
                    DataSourceString dataSourceString = new DataSourceString();
                    try{
                        connection = dataSourceString.connectDB(poolDataSource);
                    }
                    catch(Exception ex){
                        throw new Exception("DBの接続に失敗しました。application.propertiesを修正して下さい。" + ex.getMessage());
                    }
    
                    // データベース情報を取得
                    databaseProduct = dataSourceString.GetDatabaseProduct(dataSourceName);
                    databaseName = dataSourceString.GetDatabaseName(dataSourceName);
    
                    // オートコミットオフ
                    connection.setAutoCommit(false);    
                }

                // DB及びクラスのメンバーと更新後テストデータを比較
                TestData(connection, returnInfo.get("ClassName"), returnInfo.get("MethodName"), testCaseNo, databaseFlg, "Out");

                //テストケースごとにロールバック
                if(databaseFlg){
                    connection.rollback();
                }

                // 最後のテストケースだったら終了
                if (ret == 1)
                {
                    break;
                }
            }
        }
        finally{
            if(connection != null){
                connection.close();
            }
        }
    }
}


