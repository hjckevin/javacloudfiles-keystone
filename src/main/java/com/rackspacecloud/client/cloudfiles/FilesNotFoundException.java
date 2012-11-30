/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 * ���쳣��ָ���Ķ��󲻴���ʱ�׳�
 *
 */
public class FilesNotFoundException extends FilesException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 111718445621236026L;

	/**
     * ָ���Ķ��󲻴���ʱ�׳�  
     * 
     * @param message        ���� ������Ϣ
     * @param httpHeaders    ���� ���ص�HTTPͷ��
     * @param httpStatusLine ���� ���ص�HTTP״̬��
     */
	public FilesNotFoundException(String message, Header[] httpHeaders,
			StatusLine httpStatusLine) {
		super(message, httpHeaders, httpStatusLine);
	}

}
