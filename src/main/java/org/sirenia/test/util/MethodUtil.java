package org.sirenia.test.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;

public class MethodUtil {
	public static final Class<?> objArrayClass = Object[].class;
	private static ParserConfig config = new ParserConfig();
	static{
		config.setAutoTypeSupport(true);
	}
	/**
	 * 带@type的json格式
	 * @param args
	 * @return
	 */
	public static Object[] parseJSONForArgsWithType(String args){
		return JSON.parseObject(args,objArrayClass , config);
	}
	/**
	 * 不带@type的json格式
	 * @param method
	 * @param argsJSONArray
	 * @return
	 */
	public static Object[] parseJSONForArgs(Method method,String argsJSONArray){
    	Class<?>[] pts = method.getParameterTypes();
		Type[] argTypes = method.getGenericParameterTypes();
    	List<Object> list = JSON.parseArray(argsJSONArray,argTypes);
    	Object[] args = new Object[list.size()];
    	/*DefaultJSONParser存在缺陷，如下
    	 *  if (type instanceof Class) {
                            Class<?> clazz = (Class<?>) type;
                            isArray = clazz.isArray();
                            componentType = clazz.getComponentType();
                        }
    	 * 对于数组类型，反序列化时传class，则会丢失泛型信息；传type，则不会使用componentType。
    	 * */
    	for(int i=0;i<list.size();i++){
    		if(pts[i].isArray()){
    			args[i] = CollectionUtil.toArray((Object[])list.get(i), pts[i].getComponentType());
    		}else{
    			args[i] = list.get(i);
    		}
    	}
    	return args;
	}
	public static Object parseJSONForReturnType(Method method, String json) {
		//Class<?> returnType = method.getReturnType();// 获取返回值类型
		Type genericReturnType = method.getGenericReturnType();// 获取泛型返回值类型
		return JSON.parseObject(json, genericReturnType);
		//return JSONUtil.parseJSON(json, returnType, genericReturnType);
	}
	public static Method getMethodByName(String clazz, String method) {
		try {
			Class<?> klass = Class.forName(clazz);
			Method[] methods = klass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(method)) {
					return methods[i];
				}
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 对于无参方法，需要传空数组
	 * 
	 * @param clazz
	 * @param method
	 * @param argTypes
	 * @return
	 */
	public static Method getMethod(String clazz, String method, String... argTypes) {
		try {
			Class<?> klass = Class.forName(clazz);
			if (argTypes == null) {
				return klass.getMethod(method);
			}
			Class<?>[] types = new Class<?>[argTypes.length];
			for (int i = 0; i < types.length; i++) {
				types[i] = Class.forName(argTypes[i]);
			}
			return klass.getMethod(method, types);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
