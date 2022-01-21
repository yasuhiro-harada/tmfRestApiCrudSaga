package com.example.tmf;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import lombok.Getter;
import oracle.ucp.jdbc.PoolDataSource;

import java.sql.Connection;
import java.util.Calendar;
import java.util.HashMap;

@Getter
public class PatchPutMethodController{

    //====================================================================================
    // PATCH/PUT method
    //====================================================================================
    public ResData patchPutHandler(PoolDataSource poolDataSource, HashMap<String, String> bodyAttribute, String[] ids, String id, String requestUrl, String txId,
    RedisTemplate<String, Redolog> redologRedisTemplate, StringRedisTemplate lockStringRedisTemplate, String twoPhaseCommitMethod, String modifyType, HashMap<String, String> fetchData)  throws TmfException, Exception{

        Connection connection = null;
        HttpStatus httpStatus = HttpStatus.OK;
        ResData resData = new ResData();
        Redolog redolog = null;
        LockDatabase lockDatabase = new LockDatabase(lockStringRedisTemplate, twoPhaseCommitMethod);

        try{
            Database database = new Database();

            // UPDATE SQL文の作成
            ResData.ResBody resBody = database.setUpdateInsertSql(bodyAttribute, ids, requestUrl, modifyType);
            resData.getResBodies().add(resBody);

            // SqlのWHERE句を作っておく
            // ID(PrimaryKey)での検索WHERE句設定
            database.setWherePk(ids, fetchData);

            // UPDATE + WHERE SQL文の作成
            database.setUpdateSql(database.getUpdateSql() + database.getWhereSql());

            // DB接続
            connection = database.connectDB(poolDataSource);

            // オートコミットオフ
            connection.setAutoCommit(false);

            // 仮確定(2PhaseCommit)の時、更新前ログの取得
            if(StringUtils.hasLength(txId)){

                // lock
                if(twoPhaseCommitMethod.equals("tcc")){
                    lockDatabase.lock(id, txId);
                }

                // 更新前後ログを取得
                redolog = redologRedisTemplate.opsForValue().get(txId);
                // 更新前後ログが無い、またはconfirm済の場合は新しくトランザクションを開始する
                if(redolog == null || redolog.getConfirmedCalendar() != null){
                    redolog = new Redolog();
                }    
                // 更新前 Database 設定
                redolog.setBeforeUpdate(connection, ids, "PUT", fetchData);
            }

            // UPDATE文の発行
            database.excecSql(connection, database.getUpdateSql());
            
            // 仮確定(2PhaseCommit)の時、更新後ログの取得
            if(StringUtils.hasLength(txId)){

                // 更新後ログ設定
                if(twoPhaseCommitMethod.equals("tcc")){
                    redolog.getAfterSqls().add(database.getUpdateSql());
                    redolog.getAfterPlaceHolders().add(database.getPlaceHolder());
                    if(redolog.getAfterHttpStatus() == HttpStatus.I_AM_A_TEAPOT){
                        redolog.setAfterHttpStatus(HttpStatus.OK);
                    }
                    else if(redolog.getAfterHttpStatus() != HttpStatus.OK){
                        redolog.setAfterHttpStatus(HttpStatus.MULTI_STATUS);
                    }             
                }
                redolog.setRegisteredCalendar(Calendar.getInstance());

                // ReserveDBに更新前後ログを保存
                redologRedisTemplate.opsForValue().set(txId, redolog);

                // Sagaの場合はCommit + HttpStatus=OK
                if(twoPhaseCommitMethod.equals("saga")){
                    connection.commit();
                    httpStatus = HttpStatus.OK;
                }
                // TCCの場合はRollback + HttpStatus=ACCEPTED
                // Unlock用のID設定
                else if(twoPhaseCommitMethod.equals("tcc")){
                    redolog.getIds().add(id);
                    connection.rollback();
                    httpStatus = HttpStatus.ACCEPTED;
                }
            }
            // 本確定
            else{
                connection.commit();
                httpStatus = HttpStatus.OK;
            }
        }catch(TmfException ex){
            // Lockして例外が発生した場合はUnlockする
            if(redolog != null){
                lockDatabase.unlock(redolog.ids, txId);
            }
            throw ex;
        }catch(Exception ex){
            // Lockして例外が発生した場合はUnlockする
            if(redolog != null){
                lockDatabase.unlock(redolog.ids, txId);
            }
            throw new TmfException(ex.getMessage());
        }finally{
            // DBの切断
            if(connection != null){
                connection.rollback();
                connection.close();
            }
        }
        resData.setHttpStatus(httpStatus);
        return resData;
     }
}