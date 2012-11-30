/**
 * 
 */
package com.rackspacecloud.client.cloudfiles;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.HttpException;

/**
 * 该异常在客户端非法操作存储服务器端文件时抛出。
 * 
 */
@SuppressWarnings("serial")
public class FilesException extends HttpException {
	private Header[] httpHeaders;
	private StatusLine httpStatusLine;

	/**
	 * 该异常在客户端非法操作存储服务器端文件时抛出。
	 * 
	 * @param message
	 *            —— 出错信息。
	 * @param httpHeaders
	 *            —— 返回的Http头部。
	 * @param httpStatusLine
	 *            —— 返回的Http状态码。
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
	 * @return 从服务器端返回的Http头部。
	 */
	public Header[] getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * @return 更具可读性的服务器端返回的Http头部。
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
	 * @return 从服务器端返回的Http状态行。
	 */
	public StatusLine getHttpStatusLine() {
		return httpStatusLine;
	}

	/**
	 * @return 从服务器端返回的Http状态码。
	 */
	public int getHttpStatusCode() {
		return httpStatusLine == null ? -1 : httpStatusLine.getStatusCode();
	}

	/**
	 * @return 从服务器端返回的Http状态信息。
	 */
	public String getHttpStatusMessage() {
		return httpStatusLine == null ? null : httpStatusLine.getReasonPhrase();
	}

	/**
	 * @return 使用的Http版本。
	 */
	public String getHttpVersion() {
		return httpStatusLine == null ? null : httpStatusLine
				.getProtocolVersion().toString();
	}

}
