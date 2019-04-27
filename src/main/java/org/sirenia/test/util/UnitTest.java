package org.sirenia.test.util;

import java.io.File;

import org.sirenia.test.util.Callback.Callback00;

public class UnitTest {
	public static ThreadLocal<String> dataDirHolder = new InheritableThreadLocal<>();
	public static void with(String dataSetId,Callback00 cb){
		try{
			//获取数据集路径
			StackTraceElement stack = RuntimeUtil.getTestStackByTest();
			String clazzName = stack.getClassName();
			String methodName = stack.getMethodName();
			String dataDir = clazzName.replaceAll("\\.", "/")+"/"+methodName;
			//设置mock数据目录
			dataDirHolder.set(dataDir);
			//执行代码，生成sql
			generateSql(dataDir);
			//执行sql，生成数据
			executeSql(dataDir);
			cb.apply();
		}finally{
			//清除mock数据目录
			dataDirHolder.remove();
		}
		
	}
	private static void executeSql(String dataDir) {
		File sqlFile = new File(dataDir+"/init.sql");
		// TODO Auto-generated method stub
		
	}
	/*
	 * 执行代码，生成sql。执行init.cmd，生成init.sql
	 */
	private static void generateSql(String dataDir) {
		File initFile = new File(dataDir+"/init.cmd");
		
		// TODO Auto-generated method stub
		
	}
	public static void clear(String dataSetId){
		
	}
}
