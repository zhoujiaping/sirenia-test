package org.sirenia.test.start;

import java.util.List;

public class JsAspectConf {
	//private Logger logger = LoggerFactory.getLogger(this.getClass());
	private String dataHome;
	private List<String> stringSerializerClasses;
	private String clazznameRegexp;
	private String clazznameRegexpExlude;
	
	public String getClazznameRegexp() {
		return clazznameRegexp;
	}
	public void setClazznameRegexp(String clazznameRegexp) {
		this.clazznameRegexp = clazznameRegexp;
	}
	public String getClazznameRegexpExlude() {
		return clazznameRegexpExlude;
	}
	public void setClazznameRegexpExlude(String clazznameRegexpExlude) {
		this.clazznameRegexpExlude = clazznameRegexpExlude;
	}
	public List<String> getStringSerializerClasses() {
		return stringSerializerClasses;
	}
	public void setStringSerializerClasses(List<String> stringSerializerClasses) {
		this.stringSerializerClasses = stringSerializerClasses;
	}
	public String getDataHome() {
		return dataHome;
	}
	public void setDataHome(String dataHome) {
		this.dataHome = dataHome;
	}
}
