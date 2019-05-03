package org.sirenia.test.start;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sirenia.test.util.Callback.Callback10;
import org.sirenia.test.util.MyFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

/**
 * 读取dubbo配置文件，得到提供的service，遍历方法，生成测试类。
 * @author 01375156
 *
 */
public class DubboProviderTestGenerator {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private Charset charset = Charset.forName("utf-8");
	
	public void generateTest() throws FileNotFoundException{
		File file = ResourceUtils.getFile("classpath:beans");
		MyFile myfile = new MyFile(file);
		Callback10<File> cb = new Callback10<File>(){
			@Override
			public void apply(File f) {
				if(f.isDirectory()){
					return;
				}
				if(!f.getName().endsWith(".xml")){
					return;
				}
				try {
					generateTestForFile(f);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		myfile.walkBfs(cb,false);
	}
	protected void generateTestForFile(File f) throws FileNotFoundException, IOException {
		String content = StreamUtils.copyToString(new FileInputStream(f), charset );
		String reg = "<\\s*dubbo\\s*:\\s*service\\s+interface\\s+=\\s+\"(?<interfaceName>[a-zA-Z0-9\\.]+)\"";
		Pattern pattern = Pattern.compile(reg, Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(content);
		int from = 0;
		while(matcher.find(from)){
			String group = matcher.group("");
			from += group.length();
			System.out.println(group);
		}
	}
	public static void main(String[] args) throws FileNotFoundException {
		File file = ResourceUtils.getFile("classpath:");
		System.out.println(file.getAbsolutePath());
	}
}
