/*
 * See COPYING for license information.
 */ 

package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;

/**
 *  ���쳣�ڷ��ʴ洢ϵͳ�Ŀͻ��˵�¼��֤ʧ��ʱ�׳�
 */ 

public class FilesAuthorizationException extends FilesException
{
   
	private static final long serialVersionUID = -3142674319839157198L;

	/**
     * �ͻ��˵�¼��֤ʧ��ʱ�׳�  
     * 
     * @param message        ���� ������Ϣ
     * @param httpHeaders    ���� ���ص�HTTPͷ��
     * @param httpStatusLine ���� ���ص�HTTP״̬��
     */
    public FilesAuthorizationException(String message, Header [] httpHeaders, StatusLine httpStatusLine)
    {
    	super (message, httpHeaders, httpStatusLine);
    }

}
