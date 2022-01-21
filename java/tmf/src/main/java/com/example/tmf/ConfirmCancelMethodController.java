package com.example.tmf;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;

import oracle.ucp.jdbc.PoolDataSource;

import java.sql.Connection;
import java.util.Calendar;
import java.util.List;

public class ConfirmCancelMethodController{

    //====================================================================================
    // Confirm/Cancel method
    //====================================================================================
    public ResData confirmCancelHandler(PoolDataSource poolDataSource, String txId,
    RedisTemplate<String, Redolog> redologRedisTemplate, StringRedisTemplate lockStringRedisTemplate, String method, String twoPhaseCommitMethod)  throws TmfException, Exception{
    
        Connection connection = null;
        LockDatabase lockDatabase = new LockDatabase(lockStringRedisTemplate, twoPhaseCommitMethod);
        Redolog redolog = null;
        HttpStatus httpStatus = HttpStatus.OK;
        ResData resData = new ResData();

        try{

            // Redologから更新前後ログを取得
            redolog = redologRedisTemplate.opsForValue().get(txId);
            if(redolog == null){
                throw new TmfException("RedologDBに対象のTxIDが存在しない");
            }
            // confirm日時が設定されていたら2回目のconfirmはしない
            else if(method.equals("confirm") && redolog.getConfirmedCalendar() != null){
                throw new TmfException("RedologDBに対象のTxIDが存在しない");
            }

            // cancelの場合は、成功しても失敗しても、Redologから更新前後ログを削除
            if(method.equals("cancel")){
                redologRedisTemplate.delete(txId);
            }

            Database database = new Database();

            // placefolderとSQLとHTTPStatusの設定
            List<String> sqls = null;
            List<List<Database.PlaceHolder>> placeHolders = null;
            int i = 0;
            int lastIndex = 0;

            switch(method){
                case "confirm":
                    placeHolders = redolog.getAfterPlaceHolders();
                    sqls = redolog.getAfterSqls();
                    httpStatus = redolog.afterHttpStatus;
                    i = 0;
                    lastIndex = sqls.size();
                    break;
                case "cancel":
                    placeHolders = redolog.getBeforePlaceHolders();
                    sqls = redolog.getBeforeSqls();
                    httpStatus = HttpStatus.OK;
                    i = sqls.size() - 1;
                    lastIndex = -1;
                    break;
            }

            // DB接続
            connection = database.connectDB(poolDataSource);

            // オートコミットオフ
            connection.setAutoCommit(false);

            for(; i != lastIndex;){
                // placeholderを設定
                database.setPlaceHolder(placeHolders.get(i));
                // SQL文の発行
                int cnt = database.excecSql(connection, sqls.get(i));
                if(cnt <= 0 && method.equals("confirm")){
                    throw new TmfException("対象レコードなし");
                }
                if(method.equals("confirm")){
                    i++;
                }
                else if(method.equals("cancel")){
                    i--;
                }
            }

            // 本確定
            connection.commit();

            // confirmの場合は、Redologから更新前後ログを削除せず、confirm日付だけ更新する。ハウスキーピングで消す。
            if(method.equals("confirm")){
                redolog.setConfirmedCalendar(Calendar.getInstance());
                redologRedisTemplate.opsForValue().set(txId, redolog);
            }

            // Unlock
            lockDatabase.unlock(redolog.ids, txId);

        }catch(TmfException ex){
            // Lockして例外が発生した場合はUnlockする
            if(redolog != null){
                lockDatabase.unlock(redolog.ids, txId);
            }
            if(method.equals("confirm")){
                throw ex;
            }
        }catch(Exception ex){
            // Lockして例外が発生した場合はUnlockする
            if(redolog != null){
                lockDatabase.unlock(redolog.ids, txId);
            }
            if(method.equals("confirm")){
                throw new TmfException(ex.getMessage());
            }
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