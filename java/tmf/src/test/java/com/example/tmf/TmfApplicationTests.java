package com.example.tmf;


import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import scala.Console;

import com.excel.ReadGridExcel;

@SpringBootTest
class TmfApplicationTests {

	//@Test
	void contextLoads() throws Exception {


		ReadGridExcel readReadGridExcel = new ReadGridExcel("./aaa.xlsx");

		do
		{
			String cellValue  = readReadGridExcel.getNextCellValue();
			while(cellValue != null){
				System.out.println(cellValue);
				cellValue  = readReadGridExcel.getNextCellValue();
			}
		}while(readReadGridExcel.moveNextRow());

		OkHttpClient client = new OkHttpClient().newBuilder()
		.build();
	  	Request request = new Request.Builder()
		.url("http://localhost:8080/v2/products?fields=none")
		.method("GET", null)
		.build();

			Response response = client.newCall(request).execute();
			String aaa = response.body().string();
			
			String test = "["
				+"{"
				+"\"product_no\":1,"
				+"\"id\":\"1\","
				+"\"href\":\"http://localhost:8080/v2/products/1\""
				+"},"
				+"{"
				+"\"product_no\":2,"
				+"\"id\":\"2\","
				+"\"href\":\"http://localhost:8080/v2/products/2\""
				+"},"
				+"{"
				+"\"product_no\":3,"
				+"\"id\":\"3\","
				+"\"href\":\"http://localhost:8080/v2/products/3\""
				+"},"
				+"{"
				+"\"product_no\":20,"
				+"\"id\":\"20\","
				+"\"href\":\"http://localhost:8080/v2/products/20\""
				+"}"
				+"]";
			assertEquals(aaa, test);
	}
}
