/*
 * See COPYING for license information.
 */ 

package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 *  该异常在访问存储系统的客户端登录验证失败时抛出
 */ 

public class FilesAuthorizationException extends FilesException
{
   
	private static final long serialVersionUID = -3142674319839157198L;

	/**
     * 客户端登录验证失败时抛出  
     * 
     * @param message        —— 出错信息
     * @param httpHeaders    —— 返回的HTTP头部
     * @param httpStatusLine —— 返回的HTTP状态码
     */
    public FilesAuthorizationException(String message, Header [] httpHeaders, StatusLine httpStatusLine)
    {
    	super (message, httpHeaders, httpStatusLine);
    }

}
