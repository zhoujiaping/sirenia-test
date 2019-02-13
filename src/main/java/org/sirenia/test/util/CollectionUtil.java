package org.sirenia.test.util;

import java.lang.reflect.Array;
import java.util.Collection;

public class CollectionUtil {
	/**
	 * 集合类型转数组
	 * 动态指定数组的组件类型。
	 * @param collection
	 * @param componentType
	 * @return
	 */
	public static <T> T[] toArray(Collection<?> collection,Class<T> componentType){
		if(collection==null){
			return null;
		}
		T[] array = (T[]) Array.newInstance(componentType,collection.size());
		return collection.toArray(array);
	}
	/**
	 * E[]  =>   T[]
	 * 比如 Object[]转String[]，或者String[]转Object[]
	 * @param collection
	 * @param componentType
	 * @return
	 */
	public static <T,E> T[] toArray(E[] collection,Class<T> componentType){
		if(collection==null){
			return null;
		}
		T[] array = (T[]) Array.newInstance(componentType,collection.length);
		for(int i=0;i<collection.length;i++){
			Array.set(array, i, collection[i]);
		}
		return array;
	}
}
