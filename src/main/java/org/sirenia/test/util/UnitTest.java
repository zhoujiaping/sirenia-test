package org.sirenia.test.util;

import java.io.File;

public class UnitTest {
	private static ThreadLocal<String> dataDirHolder = new InheritableThreadLocal<>();
	private static ThreadLocal<String> dataSetIdHolder = new InheritableThreadLocal<>();
	public static void before(String dataSetId){
		StackTraceElement testTrace = null;
		StackTraceElement[] stes = RuntimeUtil.stackTraceElements();
		for (int i = stes.length - 1; i >= 0; i--) {
			StackTraceElement stack = stes[i];
			String clazzName = stack.getClassName();
			if (clazzName.endsWith("Test")) {
				testTrace = stack;
				break;
			}
		}
		if(testTrace==null){
			throw new RuntimeException("没有找到测试类");
		}
		//获取数据集路径
		String clazzName = testTrace.getClassName();
		//String methodName = stack.getMethodName();
		String relativeTestDir = clazzName.replaceAll("\\.", "/");
		//设置mock数据目录
		dataDirHolder.set(relativeTestDir);
		dataSetIdHolder.set(dataSetId);
		//执行代码，生成sql
		generateSql(relativeTestDir);
		//执行sql，生成数据
		executeSql(relativeTestDir);
	}
	public static void after(String dataSetId){
		dataDirHolder.remove();
		dataSetIdHolder.remove();
	}
	public static String getRelativeTestDir(){
		String relativeTestDir = dataDirHolder.get();
		if(relativeTestDir==null){
			return "";
		}
		return relativeTestDir;
	}
	public static String getDataSetId(){
		String id = dataSetIdHolder.get();
		if(id == null){
			return "";
		}
		return id;
	}
	private static void executeSql(String relativeTestDir) {
		File sqlFile = new File(relativeTestDir+"/init.sql");
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
