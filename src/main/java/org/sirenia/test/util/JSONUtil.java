package org.sirenia.test.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * json反序列化的类型信息处理方式。 
 * 方式一：指定Class，实际上使用指定Type的方式。 
 * 方式二：指定Type，动态指定。对泛型支持很好（有一个bug，用MethodUtil.parseJSONForArgs解决）。
 * 方式三：TypeReference指定：静态指定，每个类型写一遍，不能做到各类型通用。
 * 通过JSONTypeUtil.createType可以解决。
 * 
 * 方式四：序列化使用SerializerFeature.WriteClassName，反序列化使用parseconfig.setAutoTypeSupport(true);
 * 动态指定但是序列化时需要带类型的序列化。相对而言，这种方式功能最强大。
 * 考虑到泛型太复杂，泛型类、泛型方法、嵌套泛型、泛型通配符、未泛化的泛型， 
 * 以及考虑到接口类型等原因，在需要类型的时候，建议使用@type方式。
 */
public class JSONUtil {
	public static final ParserConfig parseconfig = new ParserConfig();
	static{
		parseconfig.setAutoTypeSupport(true);
	}

	/**
	 * JSON.toJSONString会自动处理对象和数组
	 * 
	 * @param obj:对象或数组或集合
	 */
	public static String toJSONStringWithType(Object obj) {
		// SerializerFeature.PrettyFormat
		return JSON.toJSONString(obj, SerializerFeature.WriteClassName);
	}

	public static <T> T parseObjectWithType(String text, Class<T> clazz) {
		//如果parseconfig没有设置支持类型，同时文本中有@type，那么会抛出异常
		T res = JSON.parseObject(text, clazz, parseconfig);
		return res;
	}
}
