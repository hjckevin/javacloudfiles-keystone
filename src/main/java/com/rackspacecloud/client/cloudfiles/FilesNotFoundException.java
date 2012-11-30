/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 * 该异常在指定的对象不存在时抛出
 *
 */
public class FilesNotFoundException extends FilesException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 111718445621236026L;

	/**
     * 指定的对象不存在时抛出  
     * 
     * @param message        —— 出错信息
     * @param httpHeaders    —— 返回的HTTP头部
     * @param httpStatusLine —— 返回的HTTP状态码
     */
	public FilesNotFoundException(String message, Header[] httpHeaders,
			StatusLine httpStatusLine) {
		super(message, httpHeaders, httpStatusLine);
	}

}
