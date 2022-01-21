package com.example.tmf;

import java.util.HashMap;
import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

//=========================================================
// Request用データ クラス
//=========================================================
@Getter
@Setter
public class ReqData implements Serializable{

    private static final long serialVersionUID = 1L;

    private String txId = "";
    private String pathId = "";
    private String method = "";
    private String confirmCancelMethod = "";
    private String offset = "";
    private String limit = "";
    private String fields = "";
    private String sort = "";
    private String filter = "";
    private HashMap<String, String> bodyAttribute = null;
}
