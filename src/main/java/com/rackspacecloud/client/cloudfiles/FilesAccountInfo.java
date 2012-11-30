/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

/**
 *  �й��˻��Ļ�����Ϣ
 */
public class FilesAccountInfo {
	private long bytesUsed;
	private int containerCount;
	
	public FilesAccountInfo(long bytes, int containers) {
		bytesUsed = bytes;
		containerCount = containers;
	}
	/**
	 * 	��ȡָ���˻��������ļ�ռ�õ��ֽ�����
	 * @return the bytesUsed
	 */
	public long getBytesUsed() {
		return bytesUsed;
	}
	/**
	 * 	�趨�˻��е����ֽ���
	 * @param bytesUsed ���� ���ֽ���ֵ��
	 */
	public void setBytesUsed(long bytesUsed) {
		this.bytesUsed = bytesUsed;
	}
	/**
	 * 
	 * 	��ȡָ���˻�������������Ŀ��
	 * @return containerCount
	 */
	public int getContainerCount() {
		return containerCount;
	}
	/**
	 * 	�趨�˻��е���������
	 * @param containerCount ���� ��������ֵ��
	 */
	public void setContainerCount(int containerCount) {
		this.containerCount = containerCount;
	}
	
	
}
