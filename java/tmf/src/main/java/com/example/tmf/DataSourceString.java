package com.example.tmf;

import java.sql.Connection;
import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;

public class DataSourceString{

    /**
     * 
     * @param poolDataSource
     * @return DB Connection
     * @throws Exception
     */
    public Connection connectDB(PoolDataSource poolDataSource) throws SQLException {

        Connection connection;

        connection = poolDataSource.getConnection();

        return connection;
    }

    /**
     * 接続文字列からRDB製品名を取得
     * @param dataSourceName 接続文字悦
     * @return RDB製品名。oracle or postgresql。
     */
    public String GetDatabaseProduct(String dataSourceName){
        String[] databaseProducts = dataSourceName.split(":");
        if(databaseProducts.length < 2){
            return "";
        }
        return databaseProducts[1];
    }

    /**
     * 接続文字列からﾃﾞｰﾀﾍﾞｰｽ名を取得
     * @param dataSourceName 接続文字列
     * @return ﾃﾞｰﾀﾍﾞｰｽ名
     */
    public String GetDatabaseName(String dataSourceName){
        // ":" and "/" で区切った一番末端の文字列がdatabase名
        String[] productNames = dataSourceName.split(":");
        if(productNames.length <= 0){
            return "";
        }
        productNames = productNames[productNames.length - 1].split("/");
        if(productNames.length <= 0){
            return "";
        }
        return productNames[productNames.length - 1];
    } 
}
