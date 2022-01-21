package com.example.tmf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.reflect.Field;
import java.net.URLDecoder;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

public class WebUtil{

    //====================================================================================
    // IDを分割
    //====================================================================================
    public static String[] splitId(String pathIds) throws TmfException,Exception{

        String[] ids = null;

        if (!StringUtils.hasLength(pathIds)){
            return ids;
        }

        // "-"区切りでIDを分割
        ids = pathIds.split("-");

        if (ids.length > DatabaseDefine.pkName.length) {
            throw new TmfException("IDの指定が誤っている");
        }
        return ids;
    }

    //====================================================================================
    // IDを結合
    //====================================================================================
    public static String concatId(String[] ids){

        String id = "";

        if (ids == null){
            return id;
        }
        if (ids.length == 0){
            return id;
        }

        for(String work : ids){
            if(StringUtils.hasLength(id)){
                id += "-";
            }
            id += work;
        }
        return id;
    }

    //====================================================================================
    // requestUrlを取得
    //====================================================================================
    public static String getrequestURL(String requestUrl, String pathIds) throws TmfException,Exception{

        requestUrl = requestUrl.substring(0, requestUrl.length() - pathIds.length());
        if(requestUrl.charAt(requestUrl.length() - 1) != '/'){
            requestUrl += "/";
        }
        return requestUrl;
    }

    //====================================================================================
    // filterを分解
    //====================================================================================
    public static String getFilter(String queryString) throws TmfException,Exception{

        String filter = "";

        if(!StringUtils.hasLength(queryString)){
            return filter;
        }

        queryString = URLDecoder.decode(queryString, "UTF-8");
        String[] querys = queryString.split("&");
        for(String query : querys){
            if(query.indexOf("fields") == 0 ||
                query.indexOf("sort") == 0 ||
                query.indexOf("limit") == 0 ||
                query.indexOf("offset") == 0){
                    continue;
            }
            if(StringUtils.hasLength(filter)){
                filter += "&";
            }
            filter += query;
        }
        return filter;
    }
    //====================================================================================
    // request Bodyの主キー項目からidを取得
    //====================================================================================
    public static String[] getIds(HttpServletRequest request) throws TmfException,Exception{

        List<String> listId = new ArrayList<String>();

        try{
            for(String name : DatabaseDefine.pkName){
                String value = request.getParameter(name);
                if(!StringUtils.hasLength(value)){
                    throw new TmfException("RequestBody内の必須項目が指定されていない");
                }
                listId.add(value);
            }
        }catch(TmfException ex){
            throw ex;
        }catch(Exception ex){
            throw new TmfException("Bodyの指定が誤っている");
        }
        return listId.toArray(new String[listId.size()]);
    }

    //====================================================================================
    // request Bodyに設定された項目名と値を設定
    //====================================================================================
    public static HashMap<String, String> getBodyAttribute(HttpServletRequest request) throws TmfException,Exception{

        HashMap<String, String> hashmap = new HashMap<String, String>();

        try{
            for(Field field : FetchData.class.getFields()){
                String name = field.getName();  
                String value = request.getParameter(name);
                if(StringUtils.hasLength(value)){
                    hashmap.put(name, value);
                }
            }
        }catch(Exception ex){
            throw new TmfException("Bodyの指定が誤っている");
        }
        return hashmap;
    }

    //====================================================================================
    // FetchDataの項目名とデータ型を設定
    //====================================================================================
    public static HashMap<String, String> getFetchData()
    {
        HashMap<String, String> hashmap = new HashMap<String, String>();
        for(Field field : FetchData.class.getFields()){
            String name = field.getName();
            String dataType = field.getType().getSimpleName();
            hashmap.put(name, dataType);
        }
        return hashmap;
    }
}