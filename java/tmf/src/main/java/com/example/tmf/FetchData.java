package com.example.tmf;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

//=========================================================
// Database Model クラス
// 利用可能なデータ型
// Integer/int
// Long/long
// Double/double
// Float/float
// String
// Date         						import java.sql.Date;       が必要
// Timestamp(yyyy-MM-dd HH:mm:ss)    	import java.sql.Timestamp;  が必要
// Date(yyyy-MM-dd)    					import java.sql.Date;  		が必要
// Time(HH:mm:ss)    					import java.sql.Time;  		が必要
//=========================================================
@Getter
@Setter
public class FetchData{
	public int product_no;
	public String name;
	public float price;
	public Timestamp createdate;
}

