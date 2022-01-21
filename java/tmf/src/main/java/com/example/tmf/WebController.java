package com.example.tmf;

import java.util.HashMap;
import java.util.List;
import java.net.URLDecoder;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import oracle.ucp.jdbc.PoolDataSource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/" + ApiVersion.version + "/" + DatabaseDefine.tableName)
public class WebController{

    // application.propertiesの値を設定
    @Value("${rdbms.databaseproduct}") String databaseproduct;
    @Value("${rdbms.twoPhaseCommitMethod}") String twoPhaseCommitMethod;

    // コネクションプール
    @Autowired
    private PoolDataSource poolDataSource;

    // Lock用 RedisTemplate
    @Autowired
    @Qualifier("getLockRedisTemplate")
    private StringRedisTemplate lockStringRedisTemplate;

    // Redlog用 RedisTemplate
    @Autowired
    @Qualifier("getRedologRedisTemplate")
    private  RedisTemplate<String, Redolog> redologRedisTemplate;

    private HashMap<String, String> fetchData = WebUtil.getFetchData();

    //====================================================================================
    // confirm handler
    //====================================================================================
    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(HttpServletRequest request) throws TmfException, Exception{
       
        // TxIDを取得(2Phase Commit)
        String txId = request.getHeader("TxID");
        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
        if(!StringUtils.hasLength(txId)){
            throw new TmfException("HeaderにTxIdが指定されていない");
        }
        else if(!(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
        }
   
        ConfirmCancelMethodController confirmCancelMethodController = new ConfirmCancelMethodController();
        ResData resData = confirmCancelMethodController.confirmCancelHandler(poolDataSource, txId, redologRedisTemplate, lockStringRedisTemplate, "confirm", twoPhaseCommitMethod);
        return ResponseEntity.status(resData.getHttpStatus()).body("");
    }

    //====================================================================================
    // cancel handler
    //====================================================================================
    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(HttpServletRequest request) throws TmfException, Exception{
       
        // TxIDを取得(2Phase Commit)
        String txId = request.getHeader("TxID");
        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
        if(!StringUtils.hasLength(txId)){
            throw new TmfException("HeaderにTxIdが指定されていない");
        }
        else if(!(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
        }
 
        ConfirmCancelMethodController confirmCancelMethodController = new ConfirmCancelMethodController();
        ResData resData = confirmCancelMethodController.confirmCancelHandler(poolDataSource, txId, redologRedisTemplate, lockStringRedisTemplate, "cancel", twoPhaseCommitMethod);
        return ResponseEntity.status(resData.getHttpStatus()).body("");
    }

    //====================================================================================
    // All GET handler
    //====================================================================================
     @GetMapping
     public ResponseEntity<List<ResData.ResBody>> getallHandler(HttpServletRequest request) throws TmfException, Exception{
        return getGetallHandler(request, "");
    }
    //====================================================================================
    // GET handler
    //====================================================================================
    @GetMapping("{pathIds}")
    public ResponseEntity<List<ResData.ResBody>> getHandler(HttpServletRequest request, @PathVariable String pathIds) throws TmfException, Exception{
        return getGetallHandler(request, pathIds);
    }
    //====================================================================================
    // GET/All GET handler
    //====================================================================================
    private ResponseEntity<List<ResData.ResBody>> getGetallHandler(HttpServletRequest request, String pathIds) throws TmfException, Exception{

        // Query Stringを分解
        String offset = request.getParameter("offset");
        String limit = request.getParameter("limit");
        String fields = request.getParameter("fields");
        String sort = request.getParameter("sort");
        String filter = WebUtil.getFilter(request.getQueryString());
        // IDを分割
        String[] ids = WebUtil.splitId(pathIds);
        // requestUrlを取得
        String requestUrl = WebUtil.getrequestURL(request.getRequestURL().toString(), pathIds);
        requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
        //requestのFullPathを設定
        String requestFullPath = request.getRequestURL().toString();
        if(StringUtils.hasLength(request.getQueryString())){
            requestFullPath += "?" + request.getQueryString();
        }
        requestFullPath = URLDecoder.decode(requestFullPath, "UTF-8");

        GetMethodController getMethodController = new GetMethodController();
        ResData resData =  getMethodController.getHandler(databaseproduct, poolDataSource, offset, limit, fields, sort, filter, ids, requestUrl, requestFullPath, fetchData, "web");
        return ResponseEntity.status(resData.getHttpStatus()).headers(resData.getHttpHeaders()).body(resData.getResBodies());
    }

    //====================================================================================
    // DELETET handler
    //====================================================================================
    @DeleteMapping("{pathIds}")
    public ResponseEntity<String> deleteHandler(HttpServletRequest request, @PathVariable String pathIds) throws TmfException, Exception{
    
        // TxIDを取得(2Phase Commit)
        String txId = request.getHeader("TxID");
        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
        if(StringUtils.hasLength(txId) && !(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
        }
        // IDを分割
        String[] ids = WebUtil.splitId(pathIds);
        if (ids.length != DatabaseDefine.pkName.length) {
            throw new TmfException("IDの指定が誤っている");
        }
        // requestUrlを取得
        String requestUrl = WebUtil.getrequestURL(request.getRequestURL().toString(), pathIds);
        requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
     
        DeleteMethodController deleteMethodController = new DeleteMethodController();
        ResData resData = deleteMethodController.deleteHandler(poolDataSource, ids, pathIds, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, fetchData);
        return ResponseEntity.status(resData.getHttpStatus()).body("");
    }

    //====================================================================================
    // POST handler
    //====================================================================================
    @PostMapping()
    public ResponseEntity<ResData.ResBody> postHandler(HttpServletRequest request) throws TmfException, Exception{
    
        // TxIDを取得(2Phase Commit)
        String txId = request.getHeader("TxID");
        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
        if(StringUtils.hasLength(txId) && !(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
        }
        // requestUrlを取得
        String requestUrl = WebUtil.getrequestURL(request.getRequestURL().toString(), "");
        requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
        // request Bodyの主キー項目からidを取得
        String[] ids = WebUtil.getIds(request);
        // IDを結合
        String pathIds = WebUtil.concatId(ids);
        // request Bodyに設定された項目名と値を設定
        HashMap<String, String> bodyAttribute = WebUtil.getBodyAttribute(request);
    
        PostMethodController postMethodController = new PostMethodController();
        ResData resData = postMethodController.postHandler(poolDataSource, bodyAttribute, ids, pathIds, requestUrl, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, fetchData);
        return ResponseEntity.status(resData.getHttpStatus()).body(resData.getResBodies().get(0));
    }

    //====================================================================================
    // PUT handler
    //====================================================================================
    @PutMapping("{pathIds}")
    public ResponseEntity<ResData.ResBody> putHandler(HttpServletRequest request, @PathVariable String pathIds) throws TmfException, Exception{
        return patchPutHandler(request, pathIds);
    }

    //====================================================================================
    // PATCH handler
    //====================================================================================
    @PatchMapping("{pathIds}")
    public ResponseEntity<ResData.ResBody> patchHandler(HttpServletRequest request, @PathVariable String pathIds) throws TmfException, Exception{
        
        return patchPutHandler(request, pathIds);
    }

    //====================================================================================
    // PATCH/PUT handler
    //====================================================================================
    public ResponseEntity<ResData.ResBody> patchPutHandler(HttpServletRequest request, String pathIds) throws TmfException, Exception{
    
        // TxIDを取得(2Phase Commit)
        String txId = request.getHeader("TxID");
        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
        if(StringUtils.hasLength(txId) && !(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
        }
        // IDを分割
        String[] ids = WebUtil.splitId(pathIds);
        if (ids.length != DatabaseDefine.pkName.length) {
            throw new TmfException("IDの指定が誤っている");
        }
        // requestUrlを取得
        String requestUrl = WebUtil.getrequestURL(request.getRequestURL().toString(), pathIds);
        requestUrl = URLDecoder.decode(requestUrl, "UTF-8");
        // request Bodyに設定された項目名と値を設定
        HashMap<String, String> bodyAttribute = WebUtil.getBodyAttribute(request);
        // request Bodyの主キー項目からidを取得
        String[] idsBody = WebUtil.getIds(request);
        if(idsBody.length > 0){
            // IDを結合
            String pathIdsBody = WebUtil.concatId(idsBody);
            if(!pathIds.equals(pathIdsBody)){
                throw new TmfException("QueryStringとRequestBodyのIDが一致しない");
            }
        }
        // methodを取得
        String method = request.getMethod();
     
        PatchPutMethodController patchPutMethodController = new PatchPutMethodController();
        ResData resData = patchPutMethodController.patchPutHandler(poolDataSource, bodyAttribute, ids, pathIds, requestUrl, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, method, fetchData);
        return ResponseEntity.status(resData.getHttpStatus()).body(resData.getResBodies().get(0));
    }
}


