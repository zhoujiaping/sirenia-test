package org.sirenia.test.util;

import java.lang.reflect.Method;

import org.junit.Test;

public class RuntimeUtil {
	/**
	 * @return
	 */
	public static StackTraceElement[] stackTraceElements() {
		Exception e = new Exception();
		return e.getStackTrace();
	}

	/**
	 * 基于Test注解判断。 有Test注解的不一定就是实际的Test方法
	 * 
	 * @return
	 */
	public static StackTraceElement getTestStackByAnno() {
		StackTraceElement[] stes = stackTraceElements();
		for (StackTraceElement stack : stes) {
			String clazzName = stack.getClassName();
			Class<?> clazz;
			try {
				clazz = Class.forName(clazzName);
				String methodName = stack.getMethodName();
				Method method = clazz.getMethod(methodName);
				Test testAnno = method.getAnnotation(Test.class);
				if (testAnno != null) {
					return stack;
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			} catch (NoSuchMethodException e) {

			}
		}
		throw new RuntimeException("没有找到测试方法");
	}

	/**
	 * 基于测试方法都是以test开头的约定。推荐使用该方式。
	 * 
	 * @return
	 */
	public static StackTraceElement getTestStackByTest() {
		StackTraceElement[] stes = stackTraceElements();
		for (int i = stes.length - 1; i >= 0; i--) {
			StackTraceElement stack = stes[i];
			String methodName = stack.getMethodName();
			if (methodName.startsWith("test")) {
				return stack;
			}
		}
		throw new RuntimeException("没有找到测试方法");
	}

	/**
	 * Test运行的时候，有固定的调用栈。但是不同的TestRunner，调用栈应该会不同。
	 * 
	 * @return
	 */
	public static StackTraceElement getTestStackInEclipse() {
		StackTraceElement[] stes = stackTraceElements();
		return stes[stes.length - 24];
	}
}
