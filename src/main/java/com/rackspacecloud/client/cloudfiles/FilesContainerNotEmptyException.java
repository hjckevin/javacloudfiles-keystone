/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 * ���쳣��ɾ������ָ����������Ϊ��ʱ�׳�
 *
 */
@SuppressWarnings("serial")
public class FilesContainerNotEmptyException extends FilesException {
	
	/**
     * ɾ������ָ����������Ϊ��ʱ�׳�  
     * 
     * @param message        ���� ������Ϣ
     * @param httpHeaders    ���� ���ص�HTTPͷ��
     * @param httpStatusLine ���� ���ص�HTTP״̬��
     */
	public FilesContainerNotEmptyException(String message,
			Header[] httpHeaders, StatusLine httpStatusLine) {
		super(message, httpHeaders, httpStatusLine);
	}
	
}
