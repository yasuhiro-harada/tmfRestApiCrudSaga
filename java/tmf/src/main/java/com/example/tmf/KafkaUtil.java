package com.example.tmf;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaUtil{

    /**
     * messageKeyをSerializeする
     * @param resData 
     * @return
     * @throws Exception
     */
    public static byte[] serializeMessageKey(MessageKey messageKey) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(messageKey).getBytes();
    }

    /**
     * resDataをSerializeする
     * @param resData 
     * @return
     * @throws Exception
     */
    public static byte[] serializeResData(ResData resData) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(resData).getBytes();
    }

    /**
     * reqDataをSerializeする
     * @param reqData 
     * @return
     * @throws Exception
     */
    public static byte[] serializeReqData(ReqData reqData) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(reqData).getBytes();
    }

    /**
     * messageKeyをDeserializeする
     * @param message
     * @return
     * @throws Exception
     */
    public static MessageKey deserializeMessageKey(byte[] message) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(message, MessageKey.class);
    }

    /**
     * resDataをDeserializeする
     * @param message
     * @return
     * @throws Exception
     */
    public static ResData deserializeResData(byte[] message) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(message, ResData.class);
    }

    /**
     * reqDataをDeserializeする
     * @param message
     * @return
     * @throws Exception
     */
    public static ReqData deserializeReqData(byte[] message) throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(message, ReqData.class);
    }

    /**
     * consumeしたmessageをproduceした順にソート
     * @param fromList ソートするmessage
     * @return
     */
    public static List<ConsumerRecord<byte[], byte[]>> sort(List<ConsumerRecord<byte[], byte[]>> fromList)
    {
        if(fromList.size() <= 1){
            return fromList;
        }
        List<ConsumerRecord<byte[], byte[]>> toList = new ArrayList<>();
        return nestedSort(fromList, toList);
    }

    /**
     * consumeしたmessageをproduceした順に再帰的にソート
     * @param fromList ソートするmessage
     * @param toList ソートされたmessage
     * @return
     */
    private static List<ConsumerRecord<byte[], byte[]>> nestedSort(List<ConsumerRecord<byte[], byte[]>> fromList, List<ConsumerRecord<byte[], byte[]>> toList)
    {
        long timestamp = fromList.get(0).timestamp();
        int minIndex = 0;
        for(int i = 1; i < fromList.size(); i++){
            if(timestamp > fromList.get(i).timestamp()){
                timestamp = fromList.get(i).timestamp();
                minIndex = i;
            }
        }
        toList.add(fromList.get(minIndex));
        fromList.remove(minIndex);
        if(fromList.size() > 0){
            nestedSort(fromList, toList);
        }
        return toList;
    }

}