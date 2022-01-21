package com.example.tmf;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import oracle.ucp.jdbc.PoolDataSource;

import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.lang.Thread;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;


@Component
@StepScope
public class MessageConsumer implements Tasklet {

    // application.propertiesの値を設定
    @Value("${controller.controllerMethod}") String controllerMethod;
    @Value("${spring.kafka.consumer.bootstrap-servers}") String consumerBootstrapServers;
    @Value("${spring.kafka.consumer.group-id}") String consumerGroupId;
    @Value("${spring.kafka.consumer.auto-offset-reset}") String consumerAutoOffsetReset;
    @Value("${spring.kafka.consumer.topic}") String consumerTopic;
    @Value("${spring.kafka.consumer.timeout}") String consumerTimeout;
    @Value("${spring.kafka.consumer.frequency}") String consumerFrequency;

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

    @Autowired
    private MessageProducer messageProducer;
    @Override
    public RepeatStatus execute(StepContribution contoribution, ChunkContext cuhnkcontext) throws Exception
    {
        // message方式でない場合はmessagePollingせずに終了
        if(!controllerMethod.equals("message")){
            return RepeatStatus.FINISHED;
        }

        KafkaConsumer<byte[], byte[]> consumer = null;

        try{
            Properties properties = new Properties();
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumerBootstrapServers);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
            
            consumer = new KafkaConsumer<>(properties, new ByteArrayDeserializer(), new ByteArrayDeserializer());
    
            List<ConsumerRecord<byte[], byte[]>> received = new ArrayList<>();
    
            consumer.subscribe(Arrays.asList(consumerTopic));
            Duration duration = Duration.ofMillis(Long.parseLong(consumerTimeout));
    
            while (received.size() <= 100) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(duration);
                if(records.count() <= 0){
                    continue;
                }
                records.forEach(received::add);
                received = KafkaUtil.sort(received);
                ReqData reqData = null;
                MessageKey messageKey = null;
                ResData resData = null;
                // MessageProducer messageProducer = new MessageProducer();
                try{
                    for(int i = 0; i < received.size(); i++){
                        reqData = null;
                        messageKey = null;
                        resData = null;
                        messageKey = KafkaUtil.deserializeMessageKey(received.get(i).key());
                        reqData = KafkaUtil.deserializeReqData(received.get(i).value());
                        resData = controller(reqData);
                        messageProducer.produce(messageKey, resData);
                    }
                    received.clear();
                    Thread.sleep(Long.parseLong(consumerFrequency));
                }catch(TmfException ex){
                    if(messageKey == null){
                        messageKey = new MessageKey();
                    }
                    messageKey.setTmfException(ex);
                    messageProducer.produce(messageKey, resData);
                }catch(Exception ex){
                    if(messageKey == null){
                        messageKey = new MessageKey();
                    }
                    messageKey.setTmfException(new TmfException(ex.getMessage()));
                    messageProducer.produce(messageKey, resData);
                }
            }
        }finally{
            if(consumer != null){
                consumer.close();
            }
        }
        return RepeatStatus.FINISHED;
    }

    private ResData controller(ReqData reqData) throws Exception, TmfException
    {
        ResData resData = null;
        // TxIDを取得(2Phase Commit)
        String txId = reqData.getTxId();
        // requestUrlを取得
        String requestUrl = "";
        // IDを取得
        String pathIds = reqData.getPathId();
        // IDを分割
        String[] ids = WebUtil.splitId(pathIds);
        // request Bodyに設定された項目名と値を設定
        HashMap<String, String> bodyAttribute = reqData.getBodyAttribute();
        // requestのFullPathを設定
        String requestFullPath = "";
        // Query Stringを分解
        String offset = reqData.getOffset();
        String limit = reqData.getLimit();
        String fields = reqData.getFields();
        String sort = reqData.getSort();
        String filter = reqData.getFilter();
        // methodを取得
        String method = reqData.getMethod();

        switch(method){

            case "POST":
                switch(reqData.getConfirmCancelMethod()){
                    case "":
                        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
                        if(StringUtils.hasLength(txId) && !(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
                            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
                        }
                    
                        PostMethodController postMethodController = new PostMethodController();
                        resData = postMethodController.postHandler(poolDataSource, bodyAttribute, ids, pathIds, requestUrl, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, fetchData);
                        break;

                    case "confirm":
                    case "cancel":
                        // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
                        if(!StringUtils.hasLength(txId)){
                            throw new TmfException("HeaderにTxIdが指定されていない");
                        }
                        else if(!(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
                            throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
                        }

                        ConfirmCancelMethodController confirmCancelMethodController = new ConfirmCancelMethodController();
                        switch(reqData.getConfirmCancelMethod()){
                            case "confirm":
                                resData = confirmCancelMethodController.confirmCancelHandler(poolDataSource, txId, redologRedisTemplate, lockStringRedisTemplate, "confirm", twoPhaseCommitMethod);
                                break;
                            case "cancel":
                                resData = confirmCancelMethodController.confirmCancelHandler(poolDataSource, txId, redologRedisTemplate, lockStringRedisTemplate, "cancel", twoPhaseCommitMethod);
                                break;
                        }
                        break;
                    default:
                        throw new TmfException("ConfirmCancelMethodに不正な値が指定されている");
                }
                break;

            case "GET":
                GetMethodController getMethodController = new GetMethodController();
                resData =  getMethodController.getHandler(databaseproduct, poolDataSource, offset, limit, fields, sort, filter, ids, requestUrl, requestFullPath, fetchData, "message");
                break;
            case "DELETE":
            case "PUT":
            case "PATCH":
                // 2PhaseCommitを利用しないかつTxIDを指定した場合はエラー
                if(StringUtils.hasLength(txId) && !(twoPhaseCommitMethod.equals("saga") || twoPhaseCommitMethod.equals("tcc"))){
                    throw new TmfException("2PhaseCommitを利用しない設定だがTxIDが指定されている");
                }
                if (ids.length != DatabaseDefine.pkName.length) {
                    throw new TmfException("IDの指定が誤っている");
                }

                switch(method){

                    case "DELETE":
                        DeleteMethodController deleteMethodController = new DeleteMethodController();
                        resData = deleteMethodController.deleteHandler(poolDataSource, ids, pathIds, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, fetchData);
                        break;
                    case "PUT":
                    case "PATCH":
                        PatchPutMethodController patchPutMethodController = new PatchPutMethodController();
                        resData = patchPutMethodController.patchPutHandler(poolDataSource, bodyAttribute, ids, pathIds, requestUrl, txId, redologRedisTemplate, lockStringRedisTemplate, twoPhaseCommitMethod, method, fetchData);
                        break;
                }
                break;
            default:
                throw new TmfException("methodに不正な値が指定されている");
        }
        return resData;
    }
}
