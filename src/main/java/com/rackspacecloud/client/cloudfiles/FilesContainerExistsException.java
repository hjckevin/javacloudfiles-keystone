/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;


/**
 * ���쳣��ָ��������������ʱ�׳�
 *
 */
public class FilesContainerExistsException extends FilesException {

	
	private static final long serialVersionUID = 7282149064519490145L;

	/**
     * ָ��������������ʱ�׳�  
     * 
     * @param message        ���� ������Ϣ
     * @param httpHeaders    ���� ���ص�HTTPͷ��
     * @param httpStatusLine ���� ���ص�HTTP״̬��
     */
	public FilesContainerExistsException(String message, Header[] httpHeaders,
			StatusLine httpStatusLine) {
		super(message, httpHeaders, httpStatusLine);
	}

}
