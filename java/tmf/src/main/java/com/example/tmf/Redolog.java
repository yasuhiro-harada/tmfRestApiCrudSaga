package com.example.tmf;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

//=========================================================
// 2PhaseCommit用 更新前後ログ クラス
//=========================================================
@Getter
@Setter
public class Redolog implements Serializable{

    // 更新前 Sql文
    public List<String> beforeSqls = new ArrayList<String>();

    // 更新前 Sql文のplaceholder
    public List<List<Database.PlaceHolder>> beforePlaceHolders = new ArrayList<List<Database.PlaceHolder>>();

    // 更新後 Sql文
    public List<String> afterSqls = new ArrayList<String>();
    // 更新後 Sql文のplaceholder
    public List<List<Database.PlaceHolder>> afterPlaceHolders = new ArrayList<List<Database.PlaceHolder>>();;
    // 更新後 HTTP Status
    public HttpStatus afterHttpStatus = HttpStatus.I_AM_A_TEAPOT;

    // Unlock用 ID
    public List<String> ids = new ArrayList<String>();
    
    // 登録日時(設定するが現在未使用)
    public Calendar registeredCalendar = null;

    // Confirm日時
    public Calendar confirmedCalendar = null;

    //=========================================================
    // 更新前 Database 設定(エラー)
    //=========================================================
    public void setBeforeUpdate(Connection connection, String[] ids, String modifyType, HashMap<String, String> fetchData) throws TmfException, Exception{

        ResData resData;
        Database database = new Database();
        HashMap<String, String> hashmapAttribue = new HashMap<String, String>();

        try{
            if(modifyType.equals("PUT") || modifyType.equals("POST")){
                // SqlのWHERE句を作っておく
                // ID(PrimaryKey)での検索WHERE句設定
                database.setWherePk(ids, fetchData);
                
                // SELECT句の作成
                database.setSelectSql("", fetchData);

                // データをフェッチ
                resData = database.fetchDBData(connection, "", fetchData);
                if(resData.getResBodies().size() == 0){
                    // 更新前レコードが存在しない
                    throw new TmfException("対象レコードなし");
                }
                //  ResData.ResBody を HashMap<String, String> に変数名と値のセットに変換
                hashmapAttribue = getHashmapAttribute(resData.getResBodies().get(0));
            }
            // 初期化
            database.getPlaceHolder().clear();
            database.setWhereSql("");
            database.setUpdateSql("");
            database.setInsertSql("");
            database.setDeleteSql("");

            // SQL文とplaceholderの作成
            switch(modifyType){

                case "PUT":
                    // UPDATE SQL文の作成
                    database.setUpdateInsertSql(hashmapAttribue, ids, "", modifyType);
                    // SqlのWHERE句を作っておく
                    // ID(PrimaryKey)での検索WHERE句設定
                    database.setWherePk(ids, fetchData);
                    // UPDATE + WHERE SQL文の作成
                    database.setUpdateSql(database.getUpdateSql() + database.getWhereSql());
                    beforeSqls.add(database.getUpdateSql());
                    beforePlaceHolders.add(database.getPlaceHolder());
                    break;

                case "POST":
                    // INSERT SQL文の作成
                    database.setUpdateInsertSql(hashmapAttribue, ids, "", modifyType);
                    beforeSqls.add(database.getInsertSql());
                    beforePlaceHolders.add(database.getPlaceHolder());
                    break;

                case "DELETE":
                    // SqlのWHERE句を作っておく
                    // ID(PrimaryKey)での検索WHERE句設定
                    database.setWherePk(ids, fetchData);
                    // DELETE + WHERE SQL文の作成
                    database.setDeleteSql("DELETE FROM " + DatabaseDefine.tableName + " " + database.getWhereSql());
                    beforeSqls.add(database.getDeleteSql());
                    beforePlaceHolders.add(database.getPlaceHolder());
                    break;
            }
        }catch(TmfException ex){
            throw ex;
        }catch(Exception ex){
            throw new TmfException(ex.getMessage());
        }
        return;
    }

    //=========================================================
    //  ResData.ResBody を HashMap<String, String> に変数名と値のセットに変換
    //=========================================================
    public HashMap<String, String> getHashmapAttribute(ResData.ResBody resBody) throws TmfException, Exception{
    
        HashMap<String, String> hashmap = new HashMap<String, String>();

        try{
            for(Field field : FetchData.class.getFields()){

                String name = field.getName();

                String value;
                if(field.get(resBody) == null){
                    value = null;
                }
                else{
                    value = field.get(resBody).toString();
                }
                hashmap.put(name, value);
            }
        }catch(Exception ex){
            throw new TmfException(ex.getMessage());
        }
        return hashmap;
    }
}