package com.example.tmf;

import java.util.HashMap;

import org.springframework.util.StringUtils;

import oracle.ucp.jdbc.PoolDataSource;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import java.sql.Connection;

public class GetMethodController{

    //====================================================================================
    // GET method
    //====================================================================================
    public ResData getHandler(String databaseProduct, PoolDataSource poolDataSource, String offset, String limit, String fields, String sort, String filter, String[] ids, String requestUrl, String requestFullPath, HashMap<String, String> fetchData, String controllerMethod)  throws TmfException, Exception{

        Connection connection = null;
        ResData resData;

        try{
            // SqlのWHERE句を作っておく
            // ID(PrimaryKey)での検索WHERE句設定
            Database database = new Database();
            database.setWherePk(ids, fetchData);

            // filterでの検索WHERE句設定
            database.setWhereFilter(filter, fetchData);

            // SELECT句の作成
            database.setSelectSql(fields, fetchData);

            // LIMIT句の作成
            int limitNum = database.setLimitSql(databaseProduct, limit);

            // OFFSET句の作成
            int offsetNum = database.setOffsetSql(databaseProduct, offset);
            
            // ORDER BY句の作成
            database.setOrderbySql(sort, fetchData);

            // DB接続
            connection = database.connectDB(poolDataSource);

            // データをフェッチ
            resData = database.fetchDBData(connection, requestUrl, fetchData);

            if((limitNum != 0 || offsetNum != 0) && controllerMethod.equals("web")){

                // 総データの件数の取得(現在のWhere句での)
                int totalCnt = database.countData(connection);
                
                // partioalデータのResponseHeader設定
                setResponseHeaderPartial(resData.getHttpHeaders(), requestFullPath, limitNum, offsetNum, limit, offset, totalCnt);
            }

            // 成功HTTPStatus設定
            resData.setHttpStatus(getSuccessHTTPStatus(limitNum, offsetNum));
            
        }catch(TmfException ex){
            throw ex;
        }catch(Exception ex){
            throw new TmfException(ex.getMessage());
        }finally{
            // DBの切断
            if(connection != null){
                connection.close();
            }
        }
        return resData;
     }

    //====================================================================================
    // Headerを設定(X-total-Count, Link) 
    //====================================================================================
    private void setResponseHeaderPartial(HttpHeaders httpHeaders, String requestFullPath, int limitNum, int offsetNum, String limit, String offset, int totalCnt)  throws TmfException,Exception{

        if(limitNum == 0){
            return;
        }
    
        // X-Total-Count設定
        httpHeaders.add("X-Total-Count", Integer.toString(totalCnt));
    
        // offsetの前の文字を探す
        String prefixOffset = "";
        String prefixPattern = "";
        if(requestFullPath.contains("&offset=" + offset)){
            // 検索文字列をエスケープ
            prefixPattern = "&";
            prefixOffset = "&";
        }else if(requestFullPath.contains("?offset=" + offset)){
            // 検索文字列をエスケープ
            prefixPattern = "\\?";
            prefixOffset = "?";
        }
    
        // rel="self"を設定
        String links = "<" + requestFullPath + ">;rel=\"self\"";
        // rel="first"を設定
        String link = "";
        if(!StringUtils.hasLength(prefixOffset)){
            link = requestFullPath + "&offset=0";
        }
        else{
            link = requestFullPath.replaceFirst(prefixPattern + "offset=" + offset, prefixOffset + "offset=0");
        }
        links += ",<" + link + ">;rel=\"first\"";
        // rel="next"を設定
        if(offsetNum < totalCnt - limitNum){
            if(!StringUtils.hasLength(prefixOffset)){
                link = requestFullPath + "&offset=" + Integer.toString(offsetNum + limitNum);
            }
            else{
                link = requestFullPath.replaceFirst(prefixPattern + "offset=" + offset, prefixOffset + "offset=" + Integer.toString(offsetNum + limitNum));
            }
            links += ",<" + link + ">;rel=\"next\"";
        }
        // rel="prev"を設定
        if(offsetNum > 0){
            int offsetNumWk = offsetNum - limitNum;
            if(offsetNumWk < 0){
                offsetNumWk = 0;
            }
            if(!StringUtils.hasLength(prefixOffset)){
                link = requestFullPath + "&offset=" + Integer.toString(offsetNumWk);
            }
            else{
                link = requestFullPath.replaceFirst(prefixPattern + "offset=" + offset, prefixOffset + "offset=" + Integer.toString(offsetNumWk));
            }
            links += ",<" + link + ">;rel=\"prev\"";
        }
        // rel="last"を設定
        int limitNumWk = totalCnt - (totalCnt % limitNum);
        if(limitNumWk == totalCnt){
            limitNumWk -= limitNum;
            if(limitNumWk < 0){
                limitNumWk = 0;
            }
        } 
        if(!StringUtils.hasLength(prefixOffset)){
            link = requestFullPath + "&offset=" + Integer.toString(limitNumWk);
        } else {
            link = requestFullPath.replaceFirst(prefixPattern + "offset=" + offset, prefixOffset + "offset=" + Integer.toString(limitNumWk));
        }
        links += ",<" + link + ">;rel=\"last\"";
    
        httpHeaders.add("Link", links);
        
        return;
    }
    //====================================================================================
    // 成功HTTPStatusを返す
    //====================================================================================
    private HttpStatus getSuccessHTTPStatus(int limitNum, int offsetNum){

        HttpStatus httpStatus;

        if(limitNum == 0 && offsetNum == 0){
            httpStatus = HttpStatus.OK;
        } else {
            httpStatus = HttpStatus.PARTIAL_CONTENT;
        }

        return httpStatus;
    }
}