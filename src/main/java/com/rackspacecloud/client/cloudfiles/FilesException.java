/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.HttpException;

/**
 * ���쳣�ڿͻ��˷Ƿ������洢���������ļ�ʱ�׳���
 * 
 */
@SuppressWarnings("serial")
public class FilesException extends HttpException {
	private Header[] httpHeaders;
	private StatusLine httpStatusLine;

	/**
	 * ���쳣�ڿͻ��˷Ƿ������洢���������ļ�ʱ�׳���
	 * 
	 * @param message
	 *            ���� ������Ϣ��
	 * @param httpHeaders
	 *            ���� ���ص�Httpͷ����
	 * @param httpStatusLine
	 *            ���� ���ص�Http״̬�롣
	 */
	public FilesException(String message, Header[] httpHeaders,
			StatusLine httpStatusLine) {
		super(message);
		this.httpHeaders = httpHeaders;
		this.httpStatusLine = httpStatusLine;
	}

	public FilesException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @return �ӷ������˷��ص�Httpͷ����
	 */
	public Header[] getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * @return ���߿ɶ��Եķ������˷��ص�Httpͷ����
	 */
	public String getHttpHeadersAsString() {
		if (httpHeaders == null)
			return "";

		StringBuffer httpHeaderString = new StringBuffer();
		for (Header h : httpHeaders)
			httpHeaderString.append(h.getName() + ": " + h.getValue() + "\n");

		return httpHeaderString.toString();
	}

	/**
	 * @return �ӷ������˷��ص�Http״̬�С�
	 */
	public StatusLine getHttpStatusLine() {
		return httpStatusLine;
	}

	/**
	 * @return �ӷ������˷��ص�Http״̬�롣
	 */
	public int getHttpStatusCode() {
		return httpStatusLine == null ? -1 : httpStatusLine.getStatusCode();
	}

	/**
	 * @return �ӷ������˷��ص�Http״̬��Ϣ��
	 */
	public String getHttpStatusMessage() {
		return httpStatusLine == null ? null : httpStatusLine.getReasonPhrase();
	}

	/**
	 * @return ʹ�õ�Http�汾��
	 */
	public String getHttpVersion() {
		return httpStatusLine == null ? null : httpStatusLine
				.getProtocolVersion().toString();
	}

}
