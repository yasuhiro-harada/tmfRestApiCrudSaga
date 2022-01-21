package com.example.tmf;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

//=========================================================
// Request用データ クラス
//=========================================================
@Getter
@Setter
public class MessageKey implements Serializable{

    private static final long serialVersionUID = 1L;

    private String messageId = "";
    private TmfException tmfException = null;
}
