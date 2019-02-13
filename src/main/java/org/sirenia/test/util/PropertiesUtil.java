package org.sirenia.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
/**
 * https://www.hellojava.com/a/158.html
 *
 */
public class PropertiesUtil {
	public static Properties loadProperties(File file){
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"))) {
			Properties prop = new Properties();
			prop.load(reader);
			return prop;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
