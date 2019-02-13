package org.sirenia.test.util.callback;
/**
 * 3个入参，1个返回值
 */

public interface Callback31<R,T1,T2,T3>{
	public R apply(T1 t1,T2 t2,T3 t3);
}
