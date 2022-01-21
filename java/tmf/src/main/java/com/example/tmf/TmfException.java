package com.example.tmf;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TmfException extends Exception{

    // warningを回避するための宣言
    private static final long serialVersionUID=1L;

    // tmf Error用構造体
    @Setter
    @Getter
    public class tmfErrorCode{
        private String code;    // tmf用エラーコード
        private String reason;  // tmf用エラーメッセージ
    }

    private tmfErrorCode tmfErrorCode;   // tmf Error用構造体
    private HttpStatus HttpStatus;       // HTTPエラーコード
    
    // コンストラクタ
    TmfException(String message){

        super(message);

        tmfErrorCode = new tmfErrorCode();
        tmfErrorCode.setReason(message);
        
        switch(tmfErrorCode.getReason()){

            case "IDの指定が誤っている":
                tmfErrorCode.setCode("E-001");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "sort属性の指定が誤っている":
                tmfErrorCode.setCode("E-002");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "offset属性の指定が誤っている":
                tmfErrorCode.setCode("E-003");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "limit属性の指定が誤っている":
                tmfErrorCode.setCode("E-004");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "filter属性の指定が誤っている":
                tmfErrorCode.setCode("E-005");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "Queryの指定が誤っている":
                tmfErrorCode.setCode("E-006");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "fields属性の指定が誤っている":
                tmfErrorCode.setCode("E-007");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "QueryStringとRequestBodyのIDが一致しない":
                tmfErrorCode.setCode("E-008");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "kafka Server 接続エラー":
                tmfErrorCode.setCode("E-009");
                HttpStatus = org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
                break;
            case "Bodyの指定が誤っている":
                tmfErrorCode.setCode("E-010");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "RequestBody内の必須項目が指定されていない":
                tmfErrorCode.setCode("E-011");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "対象レコードなし":
                tmfErrorCode.setCode("E-012");
                HttpStatus = org.springframework.http.HttpStatus.NOT_FOUND;
                break;
            case "一意制約違反":
                tmfErrorCode.setCode("E-013");
                HttpStatus = org.springframework.http.HttpStatus.CONFLICT;
                break;
            case "対象レコードがLockされている":
                tmfErrorCode.setCode("E-014");
                HttpStatus = org.springframework.http.HttpStatus.LOCKED;
                break;
            case "lockDBを他のプロセスが利用中":
                tmfErrorCode.setCode("E-015");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "RedologDBを他のプロセスが利用中":
                tmfErrorCode.setCode("E-016");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "データベースがOpenできない":
                tmfErrorCode.setCode("E-017");
                HttpStatus = org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
                break;
            case "RedologDBに対象のTxIDが存在しない":
                tmfErrorCode.setCode("E-018");
                HttpStatus = org.springframework.http.HttpStatus.NOT_FOUND;
                break;
            case "対象reserveなし":
                tmfErrorCode.setCode("E-019");
                HttpStatus = org.springframework.http.HttpStatus.NOT_FOUND;
                break;
            case "request Body属性の指定が誤っている":
                tmfErrorCode.setCode("E-020");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "対応していないDBのデータ型が利用されている":
                tmfErrorCode.setCode("E-021");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "データ型と指定された値が一致しない":
                tmfErrorCode.setCode("E-022");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "DBに存在しない列名を指定":
                tmfErrorCode.setCode("E-023");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "src/main/resources/config.jsonの読み込みに失敗":
                tmfErrorCode.setCode("E-024");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            case "2PhaseCommitを利用しない設定だがTxIDが指定されている":
                tmfErrorCode.setCode("E-025");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "HeaderにTxIdが指定されていない":
                tmfErrorCode.setCode("E-026");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "filter属性に有効な比較演算子が指定されていない":
                tmfErrorCode.setCode("E-027");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "filter属性に不正な比較演算子が指定されている":
                tmfErrorCode.setCode("E-028");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "ConfirmCancelMethodに不正な値が指定されている":
                tmfErrorCode.setCode("E-029");
                HttpStatus = org.springframework.http.HttpStatus.BAD_REQUEST;
                break;
            case "methodに不正な値が指定されている":
                tmfErrorCode.setCode("E-030");
                HttpStatus = org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
                break;
            case "Kafkaのproduceに失敗":
                tmfErrorCode.setCode("E-030");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
                
            default:
                tmfErrorCode.setCode("E-999");
                HttpStatus = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
                break;
        }
    }
}
