package com.example.tmf;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;

//=========================================================
// Lock Redis Database クラス
//=========================================================
@AllArgsConstructor
public class LockDatabase{

	private StringRedisTemplate lockRedisTemplate;
    private String twoPhaseCommitMethod;

    //====================================================================================
    // idをキーにunLock
    //====================================================================================
	public void unlock(List<String> ids, String txId)  throws TmfException, Exception{
		
        try{
            if(!twoPhaseCommitMethod.equals("tcc")){
                return;
            }
            for(String id : ids){
                // 同一txIdでlockされていればUnlock
                if(txId.equals(getTxId(id))){
                    lockRedisTemplate.delete(id);
                }
            }
        }catch(Exception ex){
            throw new TmfException(ex.getMessage());
        }
    }

    //====================================================================================
    // idをキーにtxIdを取得(Lock されていなければnulllを返す)
    //====================================================================================
	public String getTxId(String id)  throws TmfException, Exception{
		
        if(!twoPhaseCommitMethod.equals("tcc")){
            return null;
        }
        return lockRedisTemplate.opsForValue().get(id);
    }

    //====================================================================================
    // idをキーにlock
    //====================================================================================
	public void lock(String id, String txId)  throws TmfException, Exception{
		
        try{
            if(!twoPhaseCommitMethod.equals("tcc")){
                return;
            }
            // 2PhaseCommit(txId)指定でない場合は、ロックしない
            if(!StringUtils.hasLength(txId)){
                return;
            }
            if(lockRedisTemplate.opsForValue().get(id) != null){
                throw new TmfException("対象レコードがLockされている");
            }

            // lockする
            lockRedisTemplate.opsForValue().set(id, txId);

        }catch(TmfException ex){
            throw ex;
        }catch(Exception ex){
            throw new TmfException(ex.getMessage());
        }
    }
}