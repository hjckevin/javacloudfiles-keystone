/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

/**
 *  有关账户的基本信息
 */
public class FilesAccountInfo {
	private long bytesUsed;
	private int containerCount;
	
	public FilesAccountInfo(long bytes, int containers) {
		bytesUsed = bytes;
		containerCount = containers;
	}
	/**
	 * 	获取指定账户中所有文件占用的字节数。
	 * @return the bytesUsed
	 */
	public long getBytesUsed() {
		return bytesUsed;
	}
	/**
	 * 	设定账户中的总字节数
	 * @param bytesUsed —— 总字节数值。
	 */
	public void setBytesUsed(long bytesUsed) {
		this.bytesUsed = bytesUsed;
	}
	/**
	 * 
	 * 	获取指定账户中所含容器书目。
	 * @return containerCount
	 */
	public int getContainerCount() {
		return containerCount;
	}
	/**
	 * 	设定账户中的总容器数
	 * @param containerCount —— 总容器数值。
	 */
	public void setContainerCount(int containerCount) {
		this.containerCount = containerCount;
	}
	
	
}
