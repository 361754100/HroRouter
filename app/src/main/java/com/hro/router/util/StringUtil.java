package com.hro.router.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具类
 * @author Mojianzhang
 * 2016年11月11日
 */
public class StringUtil extends StringUtils {
	
	/**
	 * 把对象转换为字符串
	 * @param obj
	 * @return
	 */
	public static String toString(Object obj){
		if( obj == null ){
			return "";
		}
		return String.valueOf(obj);
	}
	
	/**
	 * 把对象转换为字符串,且去掉前后空格
	 * @param obj
	 * @return
	 */
	public static String toTrim(Object obj){
		if( obj == null || "".equals(String.valueOf(obj)) ){
			return "";
		}
		return String.valueOf(obj).trim();
	}
}
