package com.example.tmf;

import java.io.Serializable;
import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.text.SimpleDateFormat;

import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import oracle.ucp.jdbc.PoolDataSource;

//=========================================================
// Database クラス
//=========================================================
@Getter
@Setter
public class Database implements Serializable{

    // Sql文のplaceholder
    @AllArgsConstructor
    @Getter
    public class PlaceHolder implements Serializable{
        String dataType;        
        String value;
    }
    List<PlaceHolder> placeHolder = new ArrayList<PlaceHolder>();

    // Sql文のwhere句
    String whereSql = "";
    // Sql文のlimit句
    String limitSql = "";
    // Sql文のoffset句
    String offsetSql = "";
    // Sql文のOrder by句
    String sortSql = "";
    // Sql文のSelect句
    String selectSql = "*";
    // Sql文のUpdate句
    String updateSql = "*";
    // Sql文のInsert句
    String insertSql = "*";
    // Sql文のDelete句
    String deleteSql = "*";

    //====================================================================================
    // ID(PrimaryKey)での検索WHERE句作成
    //====================================================================================
    public void setWherePk(String[] ids, HashMap<String, String> fetchData) throws TmfException,Exception{

        if(ids == null){
            return;
        }

        // placeHolderの追加
        String dataType = fetchData.get(DatabaseDefine.pkName[0]);
        checkDataType(dataType, ids[0]);
        placeHolder.add(new PlaceHolder(dataType, ids[0]));
        // where句の作成
        if (!StringUtils.hasLength(whereSql)) {
            whereSql = "WHERE (";
        } else {
            whereSql += "AND (";
        }
        whereSql += DatabaseDefine.pkName[0] + " = ? ";
    
        for(int i = 1; i < ids.length; i++) {
            // placeHolderの追加
            dataType = fetchData.get(DatabaseDefine.pkName[i]);
            checkDataType(dataType, ids[i]);
            placeHolder.add(new PlaceHolder(dataType, ids[i]));
            // where句の作成
            whereSql += "AND " + DatabaseDefine.pkName[i] + " = ? ";
        }
        whereSql += ") ";

        return;
    }

    //====================================================================================
    // filterでの検索WHERE句作成
    //====================================================================================
    public void setWhereFilter(String filter, HashMap<String, String> fetchData) throws TmfException,Exception{  

        if(!StringUtils.hasLength(filter)){
            return;
        }

        // 演算子リテラル
	    final String[] operators = {
            "=>",   // error[0]
            "=<",   // error[1]
            ">=",   // ↓success[2]
            "<=",
            "<>",
            "=",
            ">",
            "<"
        };

        // AND条件の分解
        String[] ors = filter.split("&");
        for(int i = 0; i < ors.length; i++){

            // ANDが指定されたらtrue。かっこでくくる。
            boolean andFlg = false;

            // OR条件の分解(;)
            String[] or = ors[i].split(";");
            for(int j = 0; j < or.length; j++){
                
                // 比較演算子の検索
                int k = 0;
                for(; k < operators.length; k++){
                    if(!or[j].contains(operators[k])){
                        continue;
                    }
                    break;
                }
                // operatorsに指定された比較演算子が含まれていなかった
                if(k >= operators.length){
                    // 不適切なfilter属性指定のエラー処理
                    throw new TmfException("filter属性に有効な比較演算子が指定されていない");
                }
                // operatorsに間違えやすい比較演算子が含まれていた
                if(k < 2){
                    // 不適切なfilter属性指定のエラー処理
                    throw new TmfException("filter属性に不正な比較演算子が指定されている");
                }
                // 列名と値の分解
                String[] attrtValues = or[j].split(operators[k]);
                if(attrtValues.length >= 3){
                    // 不適切なfilter属性指定のエラー処理
                    throw new TmfException("filter属性に不正な比較演算子が指定されている");
                }

                // 列名を取得
                String attr = attrtValues[0];
                // 列名のチェック
                checkAttributeName(attr, fetchData);
                // FetchDataからDBのデータ型を取得
                String dataType = fetchData.get(attr);;

                // 値を取得
                String values = "";
                if(attrtValues.length == 2){
                    values = attrtValues[1];
                }
                // IN条件の分解(,)
                String[] in = values.split(",");
                for(int l = 0; l < in.length; l++){
                    // null 条件チェック
                    if(in[l].equals("null")){
                        if(!operators[k].equals("=") && !operators[k].equals("<>")){
                            // 不適切なfilter属性指定のエラー処理
                            throw new TmfException("filter属性の指定が誤っている");
                        }
                    }
                    // placeHolderの追加
                    else{
                        checkDataType(dataType, in[l]);
                        placeHolder.add(new PlaceHolder(dataType, in[l]));
                    }
                }

                // where句の作成
                if(!StringUtils.hasLength(whereSql)){
                    whereSql = getFilterWhere("WHERE(", whereSql, in, attr, operators[k]);
                    // ANDが指定されていた
                    andFlg = true;
                }
                else{
                    if(j == 0){
                        whereSql = getFilterWhere("AND(", whereSql, in, attr, operators[k]);
                        // ANDが指定されていた
                        andFlg = true;
                    }
                    // OR指定があったばあい
                    else{
                        whereSql = getFilterWhere("OR", whereSql, in, attr, operators[k]);
                    }
                }
            }
            // ANDが指定されていた場合にかっこを閉じる
            if(andFlg){
                whereSql += ") ";
            }
        }
        return;
    }

    //====================================================================================
    // Filter WHERE句の作成
    //====================================================================================
    private String getFilterWhere(String whereAndOr, String whereSql, String[] values, String attr, String operator) throws TmfException,Exception{
        // AND/OR句の設定
        if(values.length == 1){
            if(values[0].equals("null")){
                whereSql += whereAndOr + " " + attr + " IS ";
                if(operator.equals("<>")){
                    whereSql += "NOT ";
                }
                whereSql += "NULL ";
            }
            else{
                whereSql += whereAndOr + " " + attr + " " + operator + " ? ";
            }
        }
        // IN句の設定
        else{
            whereSql += whereAndOr + " "  + attr + " IN ( ";
            for(int i =0; i < values.length; i++){
                if(i > 0){
                    whereSql += ", ";
                }
                if(values[i].equals("null")){
                    whereSql += "NULL ";
                }
                else{
                    whereSql += "? ";
                }
            }
            whereSql += ") ";
        }
        return whereSql;
    }

    //====================================================================================
    // LIMIT句の作成
    //====================================================================================
    public int setLimitSql(String databaseProduct, String limit) throws TmfException,Exception{

        int limitNum = 0;

        if(!StringUtils.hasLength(limit)){
            return limitNum;
        }

        try{
            limitNum = Integer.parseInt(limit);
        }catch(Exception ex){
            // 不適切なlimit属性指定のエラー処理
            throw new TmfException("limit属性の指定が誤っている");
        }
        
        if(databaseProduct.equals("Oracle")){
            limitSql = "FETCH FIRST " + limit + " ROWS ONLY ";
        }
        else if(databaseProduct.equals("postgres")){
            limitSql = "LIMIT " + limit + " ";
        }

        return limitNum;
    }

    //====================================================================================
    // OFFSET句の作成
    //====================================================================================
    public int setOffsetSql(String databaseProduct, String offset) throws TmfException,Exception{

        int offsetNum = 0;

        if(!StringUtils.hasLength(offset)){
            return offsetNum;
        }

        try{
            offsetNum = Integer.parseInt(offset);
        }catch(Exception ex){
            // 不適切なoffset属性指定のエラー処理
            throw new TmfException("offset属性の指定が誤っている");
        }
        
        if(databaseProduct.equals("Oracle")){
            offsetSql = "OFFSET " + offset + " ROWS ";
        }
        else if(databaseProduct.equals("postgres")){
            offsetSql = "OFFSET " + offset + " ";
        }

        return offsetNum;
    }

    //====================================================================================
    // DB接続
    //====================================================================================
    public Connection connectDB(PoolDataSource poolDataSource) throws TmfException,Exception{

        Connection connection;
        try{
            connection = poolDataSource.getConnection();
        }catch(Exception ex){
            // DB Open エラー処理
            throw new TmfException("データベースがOpenできない");
        }

        return connection;
    }

    //====================================================================================
    // 総データの件数を返す(現在のWhere句での)
    //====================================================================================
    public int countData(Connection connection) throws TmfException,Exception{

        int totalCnt = 0;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try{
            String attr = selectSql.split(",")[0];

            String sql = "SELECT COUNT(" + attr + ") AS TOTALCOUNT FROM " + DatabaseDefine.tableName + " " + whereSql;

            // placeholderの設定
            preparedStatement = setPlaceHolder(connection, sql);

            resultSet = preparedStatement.executeQuery();
            resultSet.next();
            totalCnt = resultSet.getInt("TOTALCOUNT");
        }finally{
            if(resultSet  != null){
                resultSet.close();;
            }
            if(preparedStatement  != null){
                preparedStatement.close();;
            }
        }

        return totalCnt;
    }


    //====================================================================================
    // OREDER BY句の作成
    //====================================================================================
    public void setOrderbySql(String sort, HashMap<String, String> fetchData) throws TmfException,Exception{

        sortSql = "";

        if(!StringUtils.hasLength(sort)){
            return;
        }

        // 列名を分割
        String[] attrs = sort.split(",");

        sortSql = "ORDER BY ";
        
        for(int i = 0; i < attrs.length; i++){
        
            if(!StringUtils.hasLength(attrs[i])){
                continue;
            }
            if(i > 0){
                sortSql += ", ";
            }
            // 文頭に”-"が付いているとDESC
            String desc = "";
            if(attrs[i].charAt(0) == '-'){
                desc = "DESC ";
                attrs[i] = attrs[i].substring(1, attrs[i].length());
            }
            // 列名をチェック
            checkAttributeName(attrs[i], fetchData);
            
            sortSql += attrs[i] + " " + desc;
        }
        return;
    }

     //====================================================================================
    // SELECT句の作成
    //====================================================================================
    public void setSelectSql(String fields, HashMap<String, String> fetchData) throws TmfException,Exception{

        if(!StringUtils.hasLength(fields)){
            fields = "";
        }

        switch(fields){
            // // fields=noneの場合は、PKをSELECT句に設定
            case "none":
                selectSql = String.join(", ", DatabaseDefine.pkName);
                break;
            // 列名が指定されなかった場合は全ての列が対象
            case "":
                selectSql = "";

                for(Field field : FetchData.class.getFields()){
                    if(StringUtils.hasLength(selectSql)){
                        selectSql += ", ";
                    }
                    selectSql += field.getName();
                }
                break;
            // 列名が指定された場合は指定された列が対象
            default:
                // 必須のPKは追加
                selectSql = String.join(", ", DatabaseDefine.pkName);
                // 列名を分割
                String[] attrs = fields.split(",");
            
                for(String attr : attrs){

                    if(StringUtils.hasLength(selectSql)){
                        selectSql += ", ";
                    }
                    // 列名をチェック
                    checkAttributeName(attr, fetchData);
                    
                    selectSql += attr;
                }
                break;
        }
        selectSql += " ";
        return;
    }

    //====================================================================================
    // placeholderの設定
    //====================================================================================
    private PreparedStatement setPlaceHolder(Connection connection, String sql) throws TmfException,Exception{

        PreparedStatement preparedStatement = null;

        try{
            preparedStatement = connection.prepareStatement(sql);
            SimpleDateFormat simpleDateFormat;
            long miliSec;

            for(int i = 0; i < placeHolder.size(); i++){
                if(placeHolder.get(i).value.equals("null")){
                    preparedStatement.setNull(i + 1, java.sql.Types.NULL);
                    continue;
                }
                switch(placeHolder.get(i).dataType){
                    case "long":
                    case "Long":
                        preparedStatement.setLong(i + 1, Long.parseLong(placeHolder.get(i).value));
                        break;
                    case "int":
                    case "Integer":
                        preparedStatement.setInt(i + 1, Integer.parseInt(placeHolder.get(i).value));
                        break;
                    case "String":
                        preparedStatement.setString(i + 1, placeHolder.get(i).value);
                        break;
                    case "double":
                    case "Double":
                        preparedStatement.setDouble(i + 1, Double.parseDouble(placeHolder.get(i).value));
                        break;
                    case "float":
                    case "Float":
                        preparedStatement.setFloat(i + 1, Float.parseFloat(placeHolder.get(i).value));
                        break;
                    case "Timestamp":
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        miliSec = simpleDateFormat.parse(placeHolder.get(i).value).getTime();
                        Timestamp timeStamp = new Timestamp(miliSec);
                        preparedStatement.setTimestamp(i + 1, timeStamp);
                        break;
                    case "Date":
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        miliSec = simpleDateFormat.parse(placeHolder.get(i).value).getTime();
                        Date date = new Date(miliSec);
                        preparedStatement.setDate(i + 1, date);
                        break;
                    case "Time":
                        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        miliSec = simpleDateFormat.parse(placeHolder.get(i).value).getTime();
                        Time time = new Time(miliSec);
                        preparedStatement.setTime(i + 1, time);
                        break;
                    default:
                        throw new TmfException("対応していないDBのデータ型が利用されている");
                }
            }
        }catch(TmfException ex){
            if(preparedStatement  != null){
                preparedStatement.close();
            }
            throw ex;
        }catch(Exception ex){
            if(preparedStatement  != null){
                preparedStatement.close();
            }
            throw new TmfException(ex.getMessage());
        }
        return preparedStatement;
    }
    //====================================================================================
    // データフェッチ
    //====================================================================================
    public ResData fetchDBData(Connection connection, String requestUrl, HashMap<String, String> fetchData) throws TmfException,Exception{

        ResData resData = new ResData();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try{
            // sql作成
            String sql = "SELECT " + selectSql + " FROM " + DatabaseDefine.tableName + " " + whereSql + sortSql + offsetSql + limitSql;

            // placeholderの設定
            preparedStatement = setPlaceHolder(connection, sql);
            // SQL発行
            resultSet = preparedStatement.executeQuery();

            // 1レコードづつFetch
            while(resultSet.next()){
                ResData.ResBody resBody = new ResData.ResBody();
                String[] attrs = selectSql.split(",");
                // 1列づつfetchDataに設定
                for(String attr : attrs){
                    // FetchDataからDBのデータ型を取得
                    attr = attr.trim();
                    // null のデータは項目ごと削除
                    if(resultSet.getObject(attr) == null){
                        continue;
                    }
                    String dataType = fetchData.get(attr);
                    switch(dataType){
                        case "long":
                        case "Long":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getLong(attr));
                            break;
                        case "int":
                        case "Integer":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getInt(attr));
                            break;
                        case "String":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getString(attr));
                            break;
                        case "double":
                        case "Double":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getDouble(attr));
                            break;
                        case "float":
                        case "Float":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getFloat(attr));
                            break;
                        case "Timestamp":
                            Timestamp timestamp = Timestamp.valueOf(resultSet.getDate(attr).toString() + " " + resultSet.getTime(attr).toString());
                            resBody.getClass().getField(attr).set(resBody, timestamp);
                            break;
                        case "Date":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getDate(attr));
                            break;
                        case "Time":
                            resBody.getClass().getField(attr).set(resBody, resultSet.getTime(attr));
                            break;
                        default:
                            throw new TmfException("対応していないDBのデータ型が利用されている");
                    }
                }
                // idの生成
                String id = "";
                for(String attr : DatabaseDefine.pkName){
                
                    if(StringUtils.hasLength(id)){
                        id += "-";
                    }
                    id += resBody.getClass().getField(attr).get(resBody).toString();
                }
                // idの設定
                resBody.setId(id);
                // hrefの設定
                if(!requestUrl.isEmpty()){
                    resBody.setHref(requestUrl + id);
                }
                // responseに追加
                resData.getResBodies().add(resBody);
            }
        }finally{
            if(resultSet  != null){
                resultSet.close();;
            }
            if(preparedStatement  != null){
                preparedStatement.close();;
            }
        }
        return resData;
    }

    //====================================================================================
    // 列名のチェック
    //====================================================================================
    private void checkAttributeName(String attribute, HashMap<String, String> fetchData) throws TmfException{

        // DBの列名をチェック
        if(fetchData.get(attribute) == null){
            // 不適切なfilter属性指定のエラー処理
            throw new TmfException("DBに存在しない列名を指定");
        }
    }

    //====================================================================================
    // 型チェック
    //====================================================================================
    public void checkDataType(String dataType, String value) throws TmfException,Exception{
        // null が指定された場合はデータ型チェックしない
        if(value.equals("null")){
            return;
        }

        boolean defaultFlg = false;
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("yyyy-MM-dd");
        try{
            switch(dataType){
                case "int":
                case "Integer":
                    Integer.parseInt(value);
                    break;
                case "String":
                    break;
                case "double":
                case "Double":
                    Double.parseDouble(value);
                    break;
                case "float":
                case "Float":
                    Float.parseFloat(value);
                    break;
                case "long":
                case "Long":
                    Long.parseLong(value);
                    break;
                case "Timestamp":
                    String work = value;
                    if(!value.contains(".")){
                        work += ".000";
                    }
                    Timestamp.valueOf(work);
                    break;
                case "Date":
                    simpleDateformat.parse(value);
                    break;
                case "Time":
                    simpleDateformat.parse(value);
                    break;
                default:
                    defaultFlg = true;
            }
        }catch(Exception ex){
            throw new TmfException("データ型と指定された値が一致しない");
        }
        if(defaultFlg){
            throw new TmfException("対応していないDBのデータ型が利用されている");
        }
    }
    //====================================================================================
    // UPDATE/INSERT/DELETE文の発行
    //====================================================================================
    public int excecSql(Connection connection, String sql) throws TmfException,Exception{

        PreparedStatement preparedStatement = null;
        int cnt = 0;

        try{

            // placeholderの設定
            preparedStatement = setPlaceHolder(connection, sql);
            
            // SQL文を実行
            cnt = preparedStatement.executeUpdate();

        }finally{
            if(preparedStatement  != null){
                preparedStatement.close();;
            }
        }
        return cnt;
    } 
    //====================================================================================
    // UPDATE/INSERT SQL文の作成
    //====================================================================================
    public ResData.ResBody setUpdateInsertSql(HashMap<String, String> bodyAttribute, String[] ids, String requestUrl, String modifyType) throws TmfException,Exception{

        ResData.ResBody resBody = new ResData.ResBody();
        SimpleDateFormat simpleDateFormat;
        long miliSec;
        String beforeInserSql = "";
        String afterInserSql = "";

        try{
            // SQL文を初期化
            updateSql = "";
            insertSql = "";

            for(Field field : FetchData.class.getFields()){
                String name = field.getName();
                String value = bodyAttribute.get(name);
                if(value == null){
                    if(modifyType.equals("POST") || modifyType.equals("PUT")){
                        value = "null";
                    }
                    else{
                        continue;
                    }
                }
                if(StringUtils.hasLength(updateSql)){
                    updateSql += ", ";    
                    beforeInserSql += ", ";    
                    afterInserSql += ", ";    
                }
                updateSql += name + " = ? ";
                beforeInserSql += name;
                afterInserSql += "? ";

                // placeHolderの追加
                String dataType = field.getType().getSimpleName();
                checkDataType(dataType, value);
                placeHolder.add(new PlaceHolder(dataType, value));

                // nullは項目ごと削除
                if(value.equals("null")){
                    continue;
                }

                // resDataの設定
                switch(dataType){
                    case "int":
                    case "Integer":
                        resBody.getClass().getField(name).set(resBody, Integer.parseInt(value));
                        break;
                    case "String":
                        resBody.getClass().getField(name).set(resBody, value);
                        break;
                    case "double":
                    case "Double":
                        resBody.getClass().getField(name).set(resBody, Double.parseDouble(value));
                        break;
                    case "float":
                    case "Float":
                        resBody.getClass().getField(name).set(resBody, Float.parseFloat(value));
                        break;
                    case "long":
                    case "Long":
                        resBody.getClass().getField(name).set(resBody, Long.parseLong(value));
                        break;
                    case "Timestamp":
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        miliSec = simpleDateFormat.parse(value).getTime();
                        Timestamp timeStamp = new Timestamp(miliSec);
                        resBody.getClass().getField(name).set(resBody, timeStamp);
                        break;
                    case "Date":
                        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        miliSec = simpleDateFormat.parse(value).getTime();
                        Date date = new Date(miliSec);
                        resBody.getClass().getField(name).set(resBody, date);
                        break;
                    case "Time":
                        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                        miliSec = simpleDateFormat.parse(value).getTime();
                        Time time = new Time(miliSec);
                        resBody.getClass().getField(name).set(resBody, time);
                        break;
                    default:
                        throw new TmfException("対応していないDBのデータ型が利用されている");
                }                
            }
            if(!StringUtils.hasLength(updateSql)){
                throw new TmfException("Bodyの指定が誤っている");
            }
            updateSql = "UPDATE " + DatabaseDefine.tableName + " SET " + updateSql;
            insertSql = "INSERT INTO " + DatabaseDefine.tableName + "(" + beforeInserSql + ")VALUES(" + afterInserSql + ")";

            // idの生成
            String id = "";
            for(String value : ids){
            
                if(StringUtils.hasLength(id)){
                    id += "-";
                }
                id += value;
            }
            // idの設定
            resBody.setId(id);
            // hrefの設定
            if(!requestUrl.isEmpty()){
                resBody.setHref(requestUrl + id);
            }

            // SQL文を初期化
            if(modifyType.equals("POST")){
                updateSql = "";
            }
            else{
                insertSql = "";
            }

        }catch(TmfException ex){
            throw ex;
        }catch(Exception ex){
            throw new TmfException("Bodyの指定が誤っている");
        }
        return resBody;
    }
}