package org.sirenia.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.sirenia.test.util.Callback.Callback10;
/**
 * 封装文件操作
 * @author zjp
 */
public class MyFile {
	private File file;
	public MyFile(File file){
		this.file = file;
	}
	public MyFile(String filename){
		this.file = new File(filename);
	}
	public File getFile(){
		return file;
	}
	public boolean createRecursion(){
		File p = file.getParentFile();
		try {
			if(!p.exists()){
				if(!p.mkdirs()){
					return false;
				}
			}
			return file.createNewFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 递归删除该文件及其子文件
	 */
	public boolean deleteRecursion(){
		return deleteRecursion(true);
	}
	public boolean deleteRecursion(boolean deleteThisFile){
		final Map<String,Boolean> map = new HashMap<>();
		walkDfs(new Callback10<File>() {
			@Override
			public void apply(File f) {
				map.put(f.getAbsolutePath(), f.delete());
			}
		},deleteThisFile);
		return map.isEmpty();
	}
	/**
	 * 深度优先遍历
	 * */
	public void walkDfs(Callback10<File> cb) {
		walkDfs(cb,true);
	}
	/**
	 * 广度优先遍历
	 * */
	public void walkBfs(Callback10<File> cb) {
		walkBfs(cb,true);
	}
	public void walkBfs(Callback10<File> cb, boolean walkThisFile) {
		if(walkThisFile){
			cb.apply(file);
		}
		walkBfsInternal(file,cb);
	}
	/**
	 * 深度优先遍历
	 * */
	public void walkDfs(Callback10<File> cb,boolean walkThisFile) {
		walkDfsInternal(file,cb);
		if(walkThisFile){
			cb.apply(file);
		}
	}
	private void walkBfsInternal(File file,Callback10<File> cb) {
		File[] files = file.listFiles();
		for(int i=0;i<files.length;i++){
			cb.apply(files[i]);
			if(files[i].isDirectory()){
				walkBfsInternal(files[i],cb);
			}
		}
	}
	private void walkDfsInternal(File file,Callback10<File> cb) {
		File[] files = file.listFiles();
		for(int i=0;i<files.length;i++){
			if(files[i].isDirectory()){
				walkDfsInternal(files[i],cb);
			}
			cb.apply(files[i]);
		}
	}
	/**
	 * 遍历当前文件的文本行
	 */
	public void eachLine(Callback10<String> callback){
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))){
			String line = null;
			while((line = br.readLine())!=null){
				callback.apply(line);
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	/**
	 * 获取当前文件的文本内容
	 */
	public String readText(){
		StringBuilder sb = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))){
			char[] buf = new char[1024];
			int len;
			while((len = br.read(buf))>-1){
				sb.append(new String(buf,0,len));
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
		return sb.toString();
	}
	public void writeText(String text){
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"utf-8"))){
			bw.write(text);
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	public static void main(String[] args) {
		System.out.println(new MyFile("d:/upc/a/b/c/d").createRecursion());
	}
}
