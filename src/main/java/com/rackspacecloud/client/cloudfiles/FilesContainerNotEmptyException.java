/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 * 该异常在删除操作指定的容器不为空时抛出
 *
 */
@SuppressWarnings("serial")
public class FilesContainerNotEmptyException extends FilesException {
	
	/**
     * 删除操作指定的容器不为空时抛出  
     * 
     * @param message        —— 出错信息
     * @param httpHeaders    —— 返回的HTTP头部
     * @param httpStatusLine —— 返回的HTTP状态码
     */
	public FilesContainerNotEmptyException(String message,
			Header[] httpHeaders, StatusLine httpStatusLine) {
		super(message, httpHeaders, httpStatusLine);
	}
	
}
