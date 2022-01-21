package com.example.tmf;

import java.util.ArrayList;
import java.util.List;

//=========================================================
// TestData用データ クラス
// ・InTestClass.xlsxでテスト前の投入できるテストデータクラス
// ・OutTestClass.xlsxでテスト後に比較できるテストデータクラス
//=========================================================
public class TestData{
    public List<MessageKey> reqMessageKeys = new ArrayList<MessageKey>();
    public List<ReqData> reqDatas = new ArrayList<ReqData>();
    public List<MessageKey> resMessageKeys = new ArrayList<MessageKey>();
    public List<ResData> resDatas = new ArrayList<ResData>();
}
