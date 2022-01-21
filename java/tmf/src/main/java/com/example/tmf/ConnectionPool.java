package com.example.tmf;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//=========================================================
// Database Connection Pool クラス
//=========================================================
@Configuration
public class ConnectionPool{

    // application.propertiesの値を設定
    @Value("${rdbms.dataSourceName}") String dataSourceName;
    @Value("${rdbms.user}") String user;
    @Value("${rdbms.password}") String password;
    @Value("${rdbms.initialPoolSize}") int initialPoolSize;
    @Value("${rdbms.minPoolSize}") int minPoolSize;
    @Value("${rdbms.maxPoolSize}") int maxPoolSize;
    @Value("${rdbms.timeoutCheckInterval}") int timeoutCheckInterval;
    @Value("${rdbms.inactiveConnectionTimeout}") int inactiveConnectionTimeout;

    @Bean
    PoolDataSource getPoolDataSource() throws Exception {

        PoolDataSource poolDataSource;
        DataSourceString dataSourceString = new DataSourceString();

        // UCP の PoolDataSource作成
        poolDataSource = PoolDataSourceFactory.getPoolDataSource();

        // connection factory の設定
        String databaseProduct = dataSourceString.GetDatabaseProduct(dataSourceName);
        if(databaseProduct.equals("oracle")){
            poolDataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        }
        else if(databaseProduct.equals("postgresql")){
            poolDataSource.setConnectionFactoryClassName("org.postgresql.Driver");
        }
        else{
            throw new Exception("application.propertysのrdbms.dataSourceNameの値が不正");
        }
        poolDataSource.setURL(dataSourceName);
        poolDataSource.setUser(user);
        poolDataSource.setPassword(password);
        poolDataSource.setConnectionPoolName("JDBC_UCP_POOL");

        // UCPの起動時に作成される初期接続数を設定(初期値は0)
        poolDataSource.setInitialPoolSize(initialPoolSize);

        // 実行時にUCPが維持する最小の接続数を設定(初期値は0)
        poolDataSource.setMinPoolSize(minPoolSize);

        // コネクションプールで許可される最大の接続数を設定(初期値はInteger.MAX_VALUE(147483647))
        poolDataSource.setMaxPoolSize(maxPoolSize);

        // タイムアウトを秒単位で設定(初期値は30sec)
        poolDataSource.setTimeoutCheckInterval(timeoutCheckInterval);

        // コネクションプールで接続を維持する最大時間を秒単位で設定(初期値は0)
        poolDataSource.setInactiveConnectionTimeout(inactiveConnectionTimeout);
            
        return poolDataSource;
    }
}
