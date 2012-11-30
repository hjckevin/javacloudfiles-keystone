/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

/**
 * 该异常在给出的容器名或对象名不合法时抛出。
 *
 */
public class FilesInvalidNameException extends FilesException {
	/**
	 * 给出的容器名或对象名不合法时抛出
	 */
	private static final long serialVersionUID = -9043382616400647532L;

	public FilesInvalidNameException(String name) {
		super("Invalid name: " + name, null, null);
	}
	
}
