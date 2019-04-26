package org.sirenia.test.start;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsAspectConf {
	//private Logger logger = LoggerFactory.getLogger(this.getClass());
	private String appName;
	
	private String dataDir = "d:/jyd-test";
	private List<String> stringSerializerClasses;
	private String clazznameRegexp;
	private String clazznameRegexpExlude;
	public Set<String> methodSet = new HashSet<>();
	
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
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getAppName() {
		return appName;
	}
	public Set<String> getMethodSet() {
		return methodSet;
	}
	public String getDataDir() {
		return dataDir;
	}
	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}
	public String getJsSetDir(){
		return dataDir+"/method-set";
	}
	public String getJsDir(){
		return dataDir+"/method-js";
	}
	
	
}
