/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

/**
 * ���쳣�ڸ���������������������Ϸ�ʱ�׳���
 *
 */
public class FilesInvalidNameException extends FilesException {
	/**
	 * ����������������������Ϸ�ʱ�׳�
	 */
	private static final long serialVersionUID = -9043382616400647532L;

	public FilesInvalidNameException(String name) {
		super("Invalid name: " + name, null, null);
	}
	
}
