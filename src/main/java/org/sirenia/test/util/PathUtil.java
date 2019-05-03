package org.sirenia.test.util;

/**
 * @author zjp
 */
public class PathUtil {
	public static String concat(String path0,String... paths){
		StringBuilder path = new StringBuilder(path0);
		if(paths!=null){
			for(int i=0;i<paths.length-1;i++){
				path.append(paths[i]).append("/");
			}
			path.append(paths[paths.length-1]);
		}
		return path.toString().replaceAll("[/\\\\]+", "/");
	}
	public static void main(String[] args) {
		String host = "localhost";
		String port = "8080";
		String rootPath = "/a/b/\\/";
		String serviceName = "/cc";
		String serviceUrl = "http://"+PathUtil.concat(host, ":",port+"",rootPath,serviceName);
		System.out.println(serviceUrl);
		//result=>http://localhost:8080/a/b/cc
		
		String rootPath2 = "f:/";
		String subPath2 =  "text.txt";
		System.out.println(PathUtil.concat(rootPath2, subPath2));
		//result=>http://localhost:8080/a/b/cc
	}
}
