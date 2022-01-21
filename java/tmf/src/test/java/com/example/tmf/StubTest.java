package com.example.tmf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.sql.Connection;

import com.unitTest.UnitTestCommon;

// @TestPropertySource(locations="classpath:test.properties")
// @ActiveProfiles("test")
// @SpringBootTest
class StubTest extends UnitTestCommon
{
	// 単体テスト用クラスの宣言
	TestData TestClass = new TestData();	

	@Test
	void MessageCrud() throws Exception
	{
		// TestClassのListやMapや配列などはテスト前にここで確保
		for(int i = 0; i < 10; i++){
			MessageKey messageKey = new MessageKey();
			TestClass.reqMessageKeys.add(messageKey);
			ReqData reqData = new ReqData();
			TestClass.reqDatas.add(reqData);
			messageKey = new MessageKey();
			TestClass.resMessageKeys.add(messageKey);
			ResData resData = new ResData();
			TestClass.resDatas.add(resData);
		}
		InitTest(true);
	}

	public int MessageCrudTestCase(Connection connection, int testCaseNo) throws Exception
	{
		int ret = 0;

		StubMessageConsumer stubMessageConsumer = new StubMessageConsumer();
		StubMessageProducer stubMessageProducer = new StubMessageProducer();
		
		//*********************************
		// Test Case
		//*********************************
		switch(testCaseNo){

			// GET Test
			case 1:
				stubMessageProducer.produce(TestClass.reqMessageKeys.get(0), TestClass.reqDatas.get(0));
				while(true){
					stubMessageConsumer.consume(TestClass.resMessageKeys, TestClass.resDatas);
					if(TestClass.resMessageKeys.size() > 0){
						break;
					}
					Thread.sleep(10);
				}
			default:
				ret = 1;

		}

		return ret;
	}
}
