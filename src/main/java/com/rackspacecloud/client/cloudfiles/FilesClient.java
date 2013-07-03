/*
 * See COPYING for license information.
 */

package com.rackspacecloud.client.cloudfiles;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.json.JSONObject;
import org.json.JSONException;

import com.rackspacecloud.client.cloudfiles.wrapper.RequestEntityWrapper;

/**
 * 
 * �洢ϵͳ�ͻ��ˡ���������˿ͻ��˵�¼��������������󡢻�ȡ�����Լ�ɾ�����������Ȼ���������
 * ��Sample�ļ����£�������һ����Ϊ��ϸ�Ĳ���������������ġ�
 * 
 * <pre>
 * 
 *  // ���ó��淽ʽ����ͻ��ˣ��û�����"jdoe"�����룺"johnsdogsname"�� 
 * 	FilesClient myClient = FilesClient("jdoe", "johnsdogsname");
 * 
 * 
 *  // ��¼���� (<code>login()</code>������¼ʧ��ʱ����false��
 *  assert(myClient.login());
 * 
 *  // ȷ���˻��ڲ�����������
 *  assert(myClient.listContainers.length() == 0);
 *  
 *  // ����һ����������
 *  assert(myClient.createContainer("myContainer"));
 *  
 *  // �鿴�������������
 *  assert(myClient.listContainers.length() == 1);
 *  
 *  // �ϴ��ļ� "alpaca.jpg"
 *  assert(myClient.storeObject("myContainer", new File("alapca.jpg"), "image/jpeg"));
 *  
 *  // �����ļ� "alpaca.jpg"
 *  FilesObject obj = myClient.getObject("myContainer", "alpaca.jpg");
 *  byte data[] = obj.getObject();
 *  
 *  // ����������������ļ�¼��
 *  // ע��ɾ��������˳��Ҫע�⣬��������������ڵ����ж����Ժ����ɾ��������
 *  assert(myClient.deleteObject("myContainer", "alpaca.jpg"));
 *  assert(myClient.deleteContainer("myContainer");
 * </pre>
 * 
 */
public class FilesClient {
	public static final String VERSION = "v1";

	private String username = null;
	private String password = null;
	private String account = null;
	private String authenticationURL;
	private int connectionTimeOut;
	private String storageURL = null;
	private String cdnManagementURL = null;
	private String authToken = null;
	private boolean isLoggedin = false;
	private boolean useETag = true;
	private boolean snet = false;
	private String snetAddr = "https://snet-";

	private HttpClient client = null;

	private static Logger logger = Logger.getLogger(FilesClient.class);

	/**
	 * ���캯��
	 * 
	 * @param client
	 *            ���� ���ʴ洢ϵͳ��HttpClient��
	 * @param username
	 *            ���� ��¼���õ��û�����
	 * @param password
	 *            ���� ��¼���õ����롣
	 * @param authUrl
	 *            ���� �洢ϵͳ���ʵ�ַ
	 * @param account
	 *            ���� �洢ϵͳ�е��˻���
	 * @param connectionTimeOut
	 *            ���� ���ӳ�ʱ��ֵ��ms����
	 */
	public FilesClient(HttpClient client, String username, String password,
			String authUrl, String account, int connectionTimeOut) {
		this.client = client;
		this.username = username;
		this.password = password;
		this.account = account;
		if (authUrl == null) {
			authUrl = FilesUtil.getProperty("auth_url");
		}
		if (account != null && account.length() > 0) {
			this.authenticationURL = authUrl + VERSION + "/" + account
					+ FilesUtil.getProperty("auth_url_post");
		} else {
			this.authenticationURL = authUrl;
		}
		this.connectionTimeOut = connectionTimeOut;

		setUserAgent(FilesConstants.USER_AGENT);

		if (logger.isDebugEnabled()) {
			logger.debug("UserName: " + this.username);
			logger.debug("AuthenticationURL: " + this.authenticationURL);
			logger.debug("ConnectionTimeOut: " + this.connectionTimeOut);
		}
	}

	/**
	 * ���캯��
	 * 
	 * @param username
	 *            ���� ��¼���õ��û�����
	 * @param password
	 *            ���� ��¼���õ����롣
	 * @param authUrl
	 *            ���� �洢ϵͳ���ʵ�ַ��
	 * @param account
	 *            ���� �洢ϵͳ�е��˻���
	 * @param connectionTimeOut
	 *            ���� ���ӳ�ʱ��ֵ��ms����
	 */
	public FilesClient(String username, String password, String authUrl,
			String account, final int connectionTimeOut) {
		this(new DefaultHttpClient() {
			protected HttpParams createHttpParams() {
				BasicHttpParams params = new BasicHttpParams();
				org.apache.http.params.HttpConnectionParams.setSoTimeout(
						params, connectionTimeOut);
				params.setParameter("http.socket.timeout", connectionTimeOut);
				return params;
			}

			@Override
			protected ClientConnectionManager createClientConnectionManager() {
				SchemeRegistry schemeRegistry = new SchemeRegistry();
				schemeRegistry.register(new Scheme("http", 80,
						PlainSocketFactory.getSocketFactory()));
				schemeRegistry.register(new Scheme("https", 443,
						SSLSocketFactory.getSocketFactory()));
				return new ThreadSafeClientConnManager(createHttpParams(),
						schemeRegistry);
			}
		}, username, password, authUrl, account, connectionTimeOut);

	}

	/**
	 * ���캯��
	 * 
	 * @param username
	 *            ���� ��¼���õ��û�����
	 * @param password
	 *            ���� ��¼���õ����롣
	 * @param authUrl
	 *            ���� �洢ϵͳ���ʵ�ַ��
	 * 
	 */
	public FilesClient(String username, String password, String authUrl) {
		this(username, password, authUrl, null, FilesUtil
				.getIntProperty("connection_timeout"));
	}

	/**
	 * ���캯��
	 * 
	 * @param username
	 *            ���� ��¼���õ��û�����
	 * @param password
	 *            ���� ��¼���õ����롣
	 */
	public FilesClient(String username, String password) {
		this(username, password, null, null, FilesUtil
				.getIntProperty("connection_timeout"));
		// lConnectionManagerogger.warn("LGV");
		// logger.debug("LGV:" + client.getHttpConnectionManager());
	}

	/**
	 * Ĭ�Ϲ��캯��������ȫ��ͨ��cloudfiles.properties�ļ��е��趨ֵ�õ�
	 * 
	 */
	public FilesClient() {
		this(FilesUtil.getProperty("username"), FilesUtil
				.getProperty("password"), null, FilesUtil
				.getProperty("account"), FilesUtil
				.getIntProperty("connection_timeout"));
	}

	/**
	 * ������֤����ʵ�ַ�к��е��˻�����
	 * 
	 * @return The account name
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * �趨��֤���˻��ͷ��ʵ�ַ��
	 * 
	 * @param account
	 */
	public void setAccount(String account) {
		this.account = account;
		if (account != null && account.length() > 0) {
			this.authenticationURL = FilesUtil.getProperty("auth_url")
					+ VERSION + "/" + account
					+ FilesUtil.getProperty("auth_url_post");
		} else {
			this.authenticationURL = FilesUtil.getProperty("auth_url");
		}
	}

	/**
	 * ��¼������������֤�û��ĺϷ��ԡ�
	 * 
	 * @return ��½�ɹ������棬���򷵻ؼ١�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 */
	public boolean login() throws IOException, HttpException {
		HttpPost method = new HttpPost(authenticationURL);
		method.getParams().setIntParameter("http.socket.timeout",
				connectionTimeOut);

		StringEntity entity = new StringEntity(getJSONBody());
		entity.setContentType("application/json");
		method.setEntity(entity);

		FilesResponse2 response = new FilesResponse2(client.execute(method));

		if (response.loginSuccess()) {
			isLoggedin = true;
			if (usingSnet() || envSnet()) {
				storageURL = snetAddr + response.getStorageURL().substring(8);
			} else {
				storageURL = response.getStorageURL();
			}
			
//			if(storageURL.contains("172.31.201.115:8080"))
//				storageURL = storageURL.replaceAll("172.31.201.115", "127.0.0.146");
			
			
			cdnManagementURL = response.getCDNManagementURL();
			authToken = response.getAuthToken();
			logger.debug("storageURL: " + storageURL);
			logger.debug("authToken: " + authToken);
			logger.debug("cdnManagementURL:" + cdnManagementURL);
			logger.debug("ConnectionManager:" + client.getConnectionManager());
		}
		method.abort();

		return this.isLoggedin;
	}

	/**
	 * ������֤ϵͳ������Ϣ��JSON��ʽ��
	 */
	private String getJSONBody() {
		String[] tempArr = username.split(":");
		String userName, tenantName;
		userName = tempArr[0];
		tenantName = tempArr[1];

		try {
			JSONObject passwordCredentials = new JSONObject();
			passwordCredentials.put("username", userName);
			passwordCredentials.put("password", password);
			JSONObject auth = new JSONObject();
			auth.put("passwordCredentials", passwordCredentials);
			auth.put("tenantName", tenantName);
			JSONObject obj = new JSONObject();
			obj.put("auth", auth);

			return obj.toString();
		} catch (JSONException ex) {
			logger.error("Error when construction authentication body.");
		}

		return null;
	}

	/**
	 * ��¼������������֤�û��ĺϷ��ԡ�
	 * 
	 * @return ��½�ɹ������棬���򷵻ؼ١�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 */
	public boolean login(String authToken, String storageURL,
			String cdnManagmentUrl) throws IOException, HttpException {
		isLoggedin = true;
		this.storageURL = storageURL;
		this.cdnManagementURL = cdnManagmentUrl;
		this.authToken = authToken;
		return true;
	}

	/**
	 * �г������ڵ�����������Ϣ����������ʾ��
	 * 
	 * @return �����û�ָ���˻��ڵ�������Ϣ�б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainerInfo> listContainersInfo() throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainersInfo(-1, null);
	}

	/**
	 * �г������ڵ�����������Ϣ����������ʾ��
	 * 
	 * @param limit
	 *            ���� ����������Ϣ�б����󳤶ȡ�
	 * 
	 * @return �����û�ָ���˻��ڵ�������Ϣ�б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainerInfo> listContainersInfo(int limit)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listContainersInfo(limit, null);
	}

	/**
	 * �г������ڵ�����������Ϣ����������ʾ��
	 * 
	 * @param limit
	 *            ���� ����������Ϣ�б����󳤶ȡ�
	 * @param marker
	 *            ���� ������������ʱָ��������֮���������Ϣ�б�
	 * 
	 * @return �����û�ָ���˻��ڵ�������Ϣ�б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainerInfo> listContainersInfo(int limit, String marker)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		if (!this.isLoggedin()) {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		HttpGet method = null;
		try {
			LinkedList<NameValuePair> parameters = new LinkedList<NameValuePair>();
			if (limit > 0) {
				parameters.add(new BasicNameValuePair("limit", String
						.valueOf(limit)));
			}
			if (marker != null) {
				parameters.add(new BasicNameValuePair("marker", marker));
			}
			parameters.add(new BasicNameValuePair("format", "xml"));
			String uri = makeURI(storageURL, parameters);
			method = new HttpGet(uri);
			method.getParams().setIntParameter("http.socket.timeout",
					connectionTimeOut);
			method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
			FilesResponse response = new FilesResponse(client.execute(method));

			if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				method.removeHeaders(FilesConstants.X_AUTH_TOKEN);
				if (login()) {
					method = new HttpGet(uri);
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					response = new FilesResponse(client.execute(method));
				} else {
					throw new FilesAuthorizationException("Re-login failed",
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			}

			if (response.getStatusCode() == HttpStatus.SC_OK) {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(response
						.getResponseBodyAsStream());

				NodeList nodes = document.getChildNodes();
				Node accountNode = nodes.item(0);
				if (!"account".equals(accountNode.getNodeName())) {
					logger.error("Got unexpected type of XML");
					return null;
				}
				ArrayList<FilesContainerInfo> containerList = new ArrayList<FilesContainerInfo>();
				NodeList containerNodes = accountNode.getChildNodes();
				for (int i = 0; i < containerNodes.getLength(); ++i) {
					Node containerNode = containerNodes.item(i);
					if (!"container".equals(containerNode.getNodeName()))
						continue;
					String name = null;
					int count = -1;
					long size = -1;
					NodeList objectData = containerNode.getChildNodes();
					for (int j = 0; j < objectData.getLength(); ++j) {
						Node data = objectData.item(j);
						if ("name".equals(data.getNodeName())) {
							name = data.getTextContent();
						} else if ("bytes".equals(data.getNodeName())) {
							size = Long.parseLong(data.getTextContent());
						} else if ("count".equals(data.getNodeName())) {
							count = Integer.parseInt(data.getTextContent());
						} else {
							logger.debug("Unexpected container-info tag:"
									+ data.getNodeName());
						}
					}
					if (name != null) {
						FilesContainerInfo obj = new FilesContainerInfo(name,
								count, size);
						containerList.add(obj);
					}
				}
				return containerList;
			} else if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				return new ArrayList<FilesContainerInfo>();
			} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				throw new FilesNotFoundException("Account not Found",
						response.getResponseHeaders(), response.getStatusLine());
			} else {
				throw new FilesException("Unexpected Return Code",
						response.getResponseHeaders(), response.getStatusLine());
			}
		} catch (Exception ex) {
			throw new FilesException(
					"Unexpected problem, probably in parsing Server XML", ex);
		} finally {
			if (method != null)
				method.abort();
		}
	}

	/**
	 * �г������ڵ���������������������ʾ��
	 * 
	 * @return �����û�ָ���˻��ڵ��������б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainer> listContainers() throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainers(-1, null);
	}

	/**
	 * �г������ڵ���������������������ʾ��
	 * 
	 * @param limit
	 *            ���� ����������Ϣ�б����󳤶ȡ�
	 * 
	 * @return �����û�ָ���˻��ڵ��������б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainer> listContainers(int limit) throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainers(limit, null);
	}

	/**
	 * �г������ڵ���������������������ʾ��
	 * 
	 * @param limit
	 *            ���� ����������Ϣ�б����󳤶ȡ�
	 * @param marker
	 *            ���� ������������ʱָ��������֮���������Ϣ�б�
	 * 
	 * @return �����û�ָ���˻��ڵ��������б�����˻���Ϊ�գ������б���Ϊ0�� �û�δ��¼��ָ�����˻�������ʱ�����ؿա�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesContainer> listContainers(int limit, String marker)
			throws IOException, HttpException, FilesException {
		if (!this.isLoggedin()) {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		HttpGet method = null;
		try {
			LinkedList<NameValuePair> parameters = new LinkedList<NameValuePair>();

			if (limit > 0) {
				parameters.add(new BasicNameValuePair("limit", String
						.valueOf(limit)));
			}
			if (marker != null) {
				parameters.add(new BasicNameValuePair("marker", marker));
			}

			String uri = parameters.size() > 0 ? makeURI(storageURL, parameters)
					: storageURL;
			method = new HttpGet(uri);
			method.getParams().setIntParameter("http.socket.timeout",
					connectionTimeOut);
			method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
			FilesResponse response = new FilesResponse(client.execute(method));

			if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				method.abort();
				if (login()) {
					method = new HttpGet(uri);
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					response = new FilesResponse(client.execute(method));
				} else {
					throw new FilesAuthorizationException("Re-login failed",
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			}

			if (response.getStatusCode() == HttpStatus.SC_OK) {
				// logger.warn(method.getResponseCharSet());
				StrTokenizer tokenize = new StrTokenizer(
						response.getResponseBodyAsString());
				tokenize.setDelimiterString("\n");
				String[] containers = tokenize.getTokenArray();
				ArrayList<FilesContainer> containerList = new ArrayList<FilesContainer>();
				for (String container : containers) {
					containerList.add(new FilesContainer(container, this));
				}
				return containerList;
			} else if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				return new ArrayList<FilesContainer>();
			} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				throw new FilesNotFoundException("Account was not found",
						response.getResponseHeaders(), response.getStatusLine());
			} else {
				throw new FilesException("Unexpected resposne from server",
						response.getResponseHeaders(), response.getStatusLine());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new FilesException(
					"Unexpected error, probably parsing Server XML", ex);
		} finally {
			if (method != null)
				method.abort();
		}
	}

	/**
	 * �г�ָ�������ں����ض��ַ���ǰ׺�����еĶ������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param startsWith
	 *            ���� �ض��ַ���ǰ׺��
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * @param marker
	 *            ���� ���������markerָ��������֮��Ķ������б���limit����ͬʱʹ��ʵ�ֶ������б�ķ�ҳ����
	 * 
	 * @return ָ�������ں����ض��ַ���ǰ׺�����еĶ������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjectsStartingWith(String container,
			String startsWith, String path, int limit, String marker)
			throws IOException, FilesException {
		return listObjectsStartingWith(container, startsWith, path, limit,
				marker, null);
	}

	/**
	 * �г�ָ�������ں����ض��ַ���ǰ׺�����еĶ������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param startsWith
	 *            ���� �ض��ַ���ǰ׺��
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * @param marker
	 *            ���� ���������markerָ��������֮��Ķ������б���limit����ͬʱʹ��ʵ�ֶ������б�ķ�ҳ����
	 * @param delimter
	 *            ���� ָ���ض��ķָ�������Ҫ����ʵ��α���Ŀ¼�ṹ��
	 * 
	 * @return A list of FilesObjects starting with the given string
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjectsStartingWith(String container,
			String startsWith, String path, int limit, String marker,
			Character delimiter) throws IOException, FilesException {
		if (!this.isLoggedin()) {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		if (!isValidContainerName(container)) {
			throw new FilesInvalidNameException(container);
		}
		HttpGet method = null;
		try {
			LinkedList<NameValuePair> parameters = new LinkedList<NameValuePair>();
			parameters.add(new BasicNameValuePair("format", "xml"));
			if (startsWith != null) {
				parameters.add(new BasicNameValuePair(
						FilesConstants.LIST_CONTAINER_NAME_QUERY, startsWith));
			}
			if (path != null) {
				parameters.add(new BasicNameValuePair("path", path));
			}
			if (limit > 0) {
				parameters.add(new BasicNameValuePair("limit", String
						.valueOf(limit)));
			}
			if (marker != null) {
				parameters.add(new BasicNameValuePair("marker", marker));
			}
			if (delimiter != null) {
				parameters.add(new BasicNameValuePair("delimiter", delimiter
						.toString()));
			}

			String uri = parameters.size() > 0 ? makeURI(storageURL + "/"
					+ sanitizeForURI(container), parameters) : storageURL;
			method = new HttpGet(uri);
			method.getParams().setIntParameter("http.socket.timeout",
					connectionTimeOut);
			method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
			FilesResponse response = new FilesResponse(client.execute(method));

			if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				method.removeHeaders(FilesConstants.X_AUTH_TOKEN);
				if (login()) {
					method = new HttpGet(uri);
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					response = new FilesResponse(client.execute(method));
				} else {
					throw new FilesAuthorizationException("Re-login failed",
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			}

			if (response.getStatusCode() == HttpStatus.SC_OK) {
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document document = builder.parse(response
						.getResponseBodyAsStream());

				NodeList nodes = document.getChildNodes();
				Node containerList = nodes.item(0);
				if (!"container".equals(containerList.getNodeName())) {
					logger.error("Got unexpected type of XML");
					return null;
				}
				ArrayList<FilesObject> objectList = new ArrayList<FilesObject>();
				NodeList objectNodes = containerList.getChildNodes();
				for (int i = 0; i < objectNodes.getLength(); ++i) {
					Node objectNode = objectNodes.item(i);
					String nodeName = objectNode.getNodeName();
					if (!("object".equals(nodeName) || "subdir"
							.equals(nodeName)))
						continue;
					String name = null;
					String eTag = null;
					long size = -1;
					String mimeType = null;
					String lastModified = null;
					NodeList objectData = objectNode.getChildNodes();
					if ("subdir".equals(nodeName)) {
						size = 0;
						mimeType = "application/directory";
						name = objectNode.getAttributes().getNamedItem("name")
								.getNodeValue();
					}
					for (int j = 0; j < objectData.getLength(); ++j) {
						Node data = objectData.item(j);
						if ("name".equals(data.getNodeName())) {
							name = data.getTextContent();
						} else if ("content_type".equals(data.getNodeName())) {
							mimeType = data.getTextContent();
						} else if ("hash".equals(data.getNodeName())) {
							eTag = data.getTextContent();
						} else if ("bytes".equals(data.getNodeName())) {
							size = Long.parseLong(data.getTextContent());
						} else if ("last_modified".equals(data.getNodeName())) {
							lastModified = data.getTextContent();
						} else {
							logger.warn("Unexpected tag:" + data.getNodeName());
						}
					}
					if (name != null) {
						FilesObject obj = new FilesObject(name, container, this);
						if (eTag != null)
							obj.setMd5sum(eTag);
						if (mimeType != null)
							obj.setMimeType(mimeType);
						if (size >= 0)
							obj.setSize(size);
						if (lastModified != null)
							obj.setLastModified(lastModified);
						objectList.add(obj);
					}
				}
				return objectList;
			} else if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				logger.debug("Container " + container + " has no Objects");
				return new ArrayList<FilesObject>();
			} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				throw new FilesNotFoundException("Container was not found",
						response.getResponseHeaders(), response.getStatusLine());
			} else {
				throw new FilesException("Unexpected Server Result",
						response.getResponseHeaders(), response.getStatusLine());
			}
		} catch (FilesNotFoundException fnfe) {
			throw fnfe;
		} catch (Exception ex) {
			logger.error("Error parsing xml", ex);
			throw new FilesException("Error parsing server resposne", ex);
		} finally {
			if (method != null)
				method.abort();
		}
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container) throws IOException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, -1, null, null);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param delimter
	 *            ���� ָ���ض��ķָ�������Ҫ����ʵ��α���Ŀ¼�ṹ��
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, Character delimiter)
			throws IOException, FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, -1, null,
				delimiter);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, int limit)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listObjectsStartingWith(container, null, null, limit, null, null);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, String path)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listObjectsStartingWith(container, null, path, -1, null, null);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * @param delimter
	 *            ���� ָ���ض��ķָ�������Ҫ����ʵ��α���Ŀ¼�ṹ��
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, String path,
			Character delimiter) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, -1, null,
				delimiter);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, String path,
			int limit) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, limit, null);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ֻ�г�·���µĶ����б�
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * @param marker
	 *            ���� ���������markerָ��������֮��Ķ������б���limit����ͬʱʹ��ʵ�ֶ������б�ķ�ҳ����
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, String path,
			int limit, String marker) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, limit, marker);
	}

	/**
	 * �г�ָ�������ڵĶ����������б�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param limit
	 *            ���� �޶������б����󳤶ȡ�
	 * @param marker
	 *            ���� ���������markerָ��������֮��Ķ������б���limit����ͬʱʹ��ʵ�ֶ������б�ķ�ҳ����
	 * 
	 * @return ָ�������ڵĶ����������б�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public List<FilesObject> listObjects(String container, int limit,
			String marker) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, limit, marker);
	}

	/**
	 * �ж�ָ���������Ƿ���ڡ�
	 * 
	 * @param container
	 *            ���� ָ������������
	 * 
	 * @return ���ڷ����棬���򷵻ؼ١�
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 */
	public boolean containerExists(String container) throws IOException,
			HttpException {
		try {
			this.getContainerInfo(container);
			return true;
		} catch (FilesException fnfe) {
			return false;
		}
	}

	/**
	 * ��ȡָ���˻�����ϸ��Ϣ��
	 * 
	 * @return FilesAccountInfo�࣬���а�����ָ���˻�����������Ŀ�������ļ���ռ�õ��ֽ�����
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public FilesAccountInfo getAccountInfo() throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		if (this.isLoggedin()) {
			HttpHead method = null;

			try {
				method = new HttpHead(storageURL);
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
				FilesResponse response = new FilesResponse(
						client.execute(method));
				if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					method.removeHeaders(FilesConstants.X_AUTH_TOKEN);
					if (login()) {
						method.abort();
						method = new HttpHead(storageURL);
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
						response = new FilesResponse(client.execute(method));
					} else {
						throw new FilesAuthorizationException(
								"Re-login failed",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				}

				if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
					int nContainers = response.getAccountContainerCount();
					long totalSize = response.getAccountBytesUsed();
					return new FilesAccountInfo(totalSize, nContainers);
				} else {
					throw new FilesException("Unexpected return from server",
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			} finally {
				if (method != null)
					method.abort();
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * ��ȡָ�������Ļ�����Ϣ���������������������Ĵ�С��
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @return ContainerInfo�࣬���������ڶ�����Ŀ�����ж���ռ�õĿռ䡣
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public FilesContainerInfo getContainerInfo(String container)
			throws IOException, HttpException, FilesException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container)) {

				HttpHead method = null;
				try {
					method = new HttpHead(storageURL + "/"
							+ sanitizeForURI(container));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.removeHeaders(FilesConstants.X_AUTH_TOKEN);
						if (login()) {
							method = new HttpHead(storageURL + "/"
									+ sanitizeForURI(container));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}

					if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
						int objCount = response.getContainerObjectCount();
						long objSize = response.getContainerBytesUsed();
						return new FilesContainerInfo(container, objCount,
								objSize);
					} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						throw new FilesNotFoundException(
								"Container not found: " + container,
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException(
								"Unexpected result from server",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					if (method != null)
						method.abort();
				}
			} else {
				throw new FilesInvalidNameException(container);
			}
		} else
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
	}

	/**
	 * ����һ��������
	 * 
	 * @param name
	 *            ���� ����������������
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 */
	public void createContainer(String name) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		if (this.isLoggedin()) {
			if (isValidContainerName(name)) {
				HttpPut method = new HttpPut(storageURL + "/"
						+ sanitizeForURI(name));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);

				try {
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						if (login()) {
							method = new HttpPut(storageURL + "/"
									+ sanitizeForURI(name));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return;
					} else if (response.getStatusCode() == HttpStatus.SC_ACCEPTED) {
						throw new FilesContainerExistsException(name,
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException("Unexpected Response",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				throw new FilesInvalidNameException(name);
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * ɾ��ָ��������
	 * 
	 * @param name
	 *            ���� ָ����������
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 * @throws FilesInvalidNameException
	 *             ���� ָ�������������Ϸ���
	 * @throws FilesNotFoundException
	 *             ���� ָ���������������ڡ�
	 * @throws FilesContainerNotEmptyException
	 *             ���� ָ����������Ϊ�ա�
	 */
	public boolean deleteContainer(String name) throws IOException,
			HttpException, FilesAuthorizationException,
			FilesInvalidNameException, FilesNotFoundException,
			FilesContainerNotEmptyException {
		if (this.isLoggedin()) {
			if (isValidContainerName(name)) {
				HttpDelete method = new HttpDelete(storageURL + "/"
						+ sanitizeForURI(name));
				try {
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						if (login()) {
							method = new HttpDelete(storageURL + "/"
									+ sanitizeForURI(name));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}

					if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
						logger.debug("Container Deleted : " + name);
						return true;
					} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						logger.debug("Container does not exist !");
						throw new FilesNotFoundException(
								"You can't delete an non-empty container",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else if (response.getStatusCode() == HttpStatus.SC_CONFLICT) {
						logger.debug("Container is not empty, can not delete a none empty container !");
						throw new FilesContainerNotEmptyException(
								"You can't delete an non-empty container",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				throw new FilesInvalidNameException(name);
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		return false;
	}

	/**
	 * ������������һ��·������·��Ҳ�ᱻ����һ�����󵥶���š�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ָ��·��ȫ�ġ�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public void createPath(String container, String path) throws HttpException,
			IOException, FilesException {

		if (!isValidContainerName(container))
			throw new FilesInvalidNameException(container);
		if (!isValidObjectName(path))
			throw new FilesInvalidNameException(path);
		storeObject(container, new byte[0], "application/directory", path,
				new HashMap<String, String>());
	}

	/**
	 * �Ӹ�����·���������Ŀ¼·�����ø�����·���ᱻ����'/'���ŷֲ㴦�����磬����createFullPath("myContainer",
	 * "foo/bar/baz")������"myContainer"
	 * ����������"food"��"food/bar"��"foo/bar/baz"����·����ÿһ������һ�������Ķ���
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param path
	 *            ���� ָ��·��ȫ�ġ�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public void createFullPath(String container, String path)
			throws HttpException, IOException, FilesException {
		String parts[] = path.split("/");

		for (int i = 0; i < parts.length; ++i) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j <= i; ++j) {
				if (sb.length() != 0)
					sb.append("/");
				sb.append(parts[j]);
			}
			createPath(container, sb.toString());
		}

	}

	/**
	 * �ڷ������˴�����������������û��ϴ�����5G���ļ�ʱ���зֵĶ���Ƭ�Σ����������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param manifest
	 *            ���� �趨��Զ����ļ���������
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public boolean createManifestObject(String container, String contentType,
			String name, String manifest, IFilesTransferCallback callback)
			throws IOException, HttpException, FilesException {
		return createManifestObject(container, contentType, name, manifest,
				new HashMap<String, String>(), callback);
	}

	/**
	 * �ڷ������˴�����������������û��ϴ�����5G���ļ�ʱ���зֵĶ���Ƭ�Σ����������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param manifest
	 *            ���� �趨��Զ����ļ���������
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public boolean createManifestObject(String container, String contentType,
			String name, String manifest, Map<String, String> metadata)
			throws IOException, HttpException, FilesException {
		return createManifestObject(container, contentType, name, manifest,
				metadata, null);
	}

	/**
	 * �ڷ������˴�����������������û��ϴ�����5G���ļ�ʱ���зֵĶ���Ƭ�Σ����������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param manifest
	 *            ���� �趨��Զ����ļ���������
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public boolean createManifestObject(String container, String contentType,
			String name, String manifest, Map<String, String> metadata,
			IFilesTransferCallback callback) throws IOException, HttpException,
			FilesException {
		byte[] arr = new byte[0];
		if (this.isLoggedin()) {
			String objName = name;
			if (isValidContainerName(container) && isValidObjectName(objName)) {

				HttpPut method = null;
				try {
					method = new HttpPut(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(objName));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					method.setHeader(FilesConstants.MANIFEST_HEADER, manifest);
					ByteArrayEntity entity = new ByteArrayEntity(arr);
					entity.setContentType(contentType);
					method.setEntity(new RequestEntityWrapper(entity, callback));
					for (String key : metadata.keySet()) {
						// logger.warn("Key:" + key + ":" +
						// sanitizeForURI(metadata.get(key)));
						method.setHeader(FilesConstants.X_OBJECT_META + key,
								sanitizeForURI(metadata.get(key)));
					}

					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						if (login()) {
							method = new HttpPut(storageURL + "/"
									+ sanitizeForURI(container) + "/"
									+ sanitizeForURI(objName));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							if (manifest != null) {
								method.setHeader(
										FilesConstants.MANIFEST_HEADER,
										manifest);
							}
							entity = new ByteArrayEntity(arr);
							entity.setContentType(contentType);
							method.setEntity(new RequestEntityWrapper(entity,
									callback));
							for (String key : metadata.keySet()) {
								method.setHeader(FilesConstants.X_OBJECT_META
										+ key,
										sanitizeForURI(metadata.get(key)));
							}
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return true;
					} else if (response.getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED) {
						throw new FilesException("Etag missmatch",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else if (response.getStatusCode() == HttpStatus.SC_LENGTH_REQUIRED) {
						throw new FilesException("Length miss-match",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException("Unexpected Server Response",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					if (method != null)
						method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * �ڷ������˱����ļ���
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * 
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name) throws IOException, HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name,
				new HashMap<String, String>(), null);
	}

	/**
	 * �ڷ������˱����ļ���
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * 
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name, IFilesTransferCallback callback) throws IOException,
			HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name,
				new HashMap<String, String>(), callback);
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name, Map<String, String> metadata) throws IOException,
			HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name, metadata, null);
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name, Map<String, String> metadata,
			IFilesTransferCallback callback) throws IOException, HttpException,
			FilesException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(name)) {
				if (!obj.exists()) {
					throw new FileNotFoundException(name + " does not exist");
				}

				if (obj.isDirectory()) {
					throw new IOException("The alleged file was a directory");
				}

				HttpPut method = null;
				try {
					method = new HttpPut(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(name));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					if (useETag) {
						method.setHeader(FilesConstants.E_TAG, md5Sum(obj));
					}
					method.setEntity(new RequestEntityWrapper(new FileEntity(
							obj, contentType), callback));
					for (String key : metadata.keySet()) {
						method.setHeader(FilesConstants.X_OBJECT_META + key,
								sanitizeForURI(metadata.get(key)));
					}
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						if (login()) {
							method = new HttpPut(storageURL + "/"
									+ sanitizeForURI(container) + "/"
									+ sanitizeForURI(name));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							if (useETag) {
								method.setHeader(FilesConstants.E_TAG,
										md5Sum(obj));
							}
							method.setEntity(new RequestEntityWrapper(
									new FileEntity(obj, contentType), callback));
							for (String key : metadata.keySet()) {
								method.setHeader(FilesConstants.X_OBJECT_META
										+ key,
										sanitizeForURI(metadata.get(key)));
							}
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}
					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return response.getResponseHeader(FilesConstants.E_TAG)
								.getValue();
					} else if (response.getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED) {
						throw new FilesException("Etag missmatch",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else if (response.getStatusCode() == HttpStatus.SC_LENGTH_REQUIRED) {
						throw new FilesException("Length miss-match",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException("Unexpected Server Response",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					if (method != null)
						method.abort();
				}
			} else {
				if (!isValidObjectName(name)) {
					throw new FilesInvalidNameException(name);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * ���ƶ��󣬲����������ԭʼλ�á�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String storeObject(String container, File obj, String contentType)
			throws IOException, HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, obj.getName());
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public boolean storeObject(String container, byte obj[],
			String contentType, String name, Map<String, String> metadata)
			throws IOException, HttpException, FilesException {
		return storeObject(container, obj, contentType, name, metadata, null);
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param obj
	 *            ���� �����浽�������˵��ļ���
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public boolean storeObject(String container, byte obj[],
			String contentType, String name, Map<String, String> metadata,
			IFilesTransferCallback callback) throws IOException, HttpException,
			FilesException {
		if (this.isLoggedin()) {
			String objName = name;
			if (isValidContainerName(container) && isValidObjectName(objName)) {

				HttpPut method = null;
				try {
					method = new HttpPut(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(objName));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					if (useETag) {
						method.setHeader(FilesConstants.E_TAG, md5Sum(obj));
					}
					ByteArrayEntity entity = new ByteArrayEntity(obj);
					entity.setContentType(contentType);
					method.setEntity(new RequestEntityWrapper(entity, callback));
					for (String key : metadata.keySet()) {
						// logger.warn("Key:" + key + ":" +
						// sanitizeForURI(metadata.get(key)));
						method.setHeader(FilesConstants.X_OBJECT_META + key,
								sanitizeForURI(metadata.get(key)));
					}

					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						if (login()) {
							method = new HttpPut(storageURL + "/"
									+ sanitizeForURI(container) + "/"
									+ sanitizeForURI(objName));
							method.getParams().setIntParameter(
									"http.socket.timeout", connectionTimeOut);
							method.setHeader(FilesConstants.X_AUTH_TOKEN,
									authToken);
							if (useETag) {
								method.setHeader(FilesConstants.E_TAG,
										md5Sum(obj));
							}
							entity = new ByteArrayEntity(obj);
							entity.setContentType(contentType);
							method.setEntity(new RequestEntityWrapper(entity,
									callback));
							for (String key : metadata.keySet()) {
								method.setHeader(FilesConstants.X_OBJECT_META
										+ key,
										sanitizeForURI(metadata.get(key)));
							}
							response = new FilesResponse(client.execute(method));
						} else {
							throw new FilesAuthorizationException(
									"Re-login failed",
									response.getResponseHeaders(),
									response.getStatusLine());
						}
					}

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return true;
					} else if (response.getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED) {
						throw new FilesException("Etag missmatch",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else if (response.getStatusCode() == HttpStatus.SC_LENGTH_REQUIRED) {
						throw new FilesException("Length miss-match",
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException("Unexpected Server Response",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					if (method != null)
						method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݣ���������ļ����������������� ���ַ�ʽ����ʡȥ�ļ��ĳ������ã�ͬʱ���������ڴ��б��������ļ��ĸ�����
	 * 
	 * @param container
	 *            ���� ָ��������
	 * @param data
	 *            ���� ���������������
	 * @param contentType
	 *            ���� �����ļ���MIME���͡�
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @param callback
	 *            ���� �趨����Ļص�������һ��Ϊ�գ�NULL����
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 * 
	 */
	public String storeStreamedObject(String container, InputStream data,
			String contentType, String name, Map<String, String> metadata)
			throws IOException, HttpException, FilesException {
		if (this.isLoggedin()) {
			String objName = name;
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				HttpPut method = new HttpPut(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
				InputStreamEntity entity = new InputStreamEntity(data, -1);
				entity.setChunked(true);
				entity.setContentType(contentType);
				method.setEntity(entity);
				for (String key : metadata.keySet()) {
					// logger.warn("Key:" + key + ":" +
					// sanitizeForURI(metadata.get(key)));
					method.setHeader(FilesConstants.X_OBJECT_META + key,
							sanitizeForURI(metadata.get(key)));
				}
				method.removeHeaders("Content-Length");

				try {
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return response.getResponseHeader(FilesConstants.E_TAG)
								.getValue();
					} else {
						logger.error(response.getStatusLine());
						throw new FilesException("Unexpected result",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * �ڷ������˱����ļ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ��������
	 * @param name
	 *            ���� �����ļ����ļ�����
	 * @param entity
	 *            ���� ���ͱ��������ʵ������
	 * @param metadata
	 *            ���� �趨Ԫ���ݼ�ֵ�Ե�ӳ�����ݡ�
	 * @param md5sum
	 *            ���� �������ݵ�MD5ֵ����32��16�����ַ�����ʽ��ʾ��
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String storeObjectAs(String container, String name,
			HttpEntity entity, Map<String, String> metadata, String md5sum)
			throws IOException, HttpException, FilesException {
		if (this.isLoggedin()) {
			String objName = name;
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				HttpPut method = new HttpPut(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
				method.setEntity(entity);
				if (useETag && md5sum != null) {
					method.setHeader(FilesConstants.E_TAG, md5sum);
				}
				method.setHeader(entity.getContentType());

				for (String key : metadata.keySet()) {
					method.setHeader(FilesConstants.X_OBJECT_META + key,
							sanitizeForURI(metadata.get(key)));
				}

				try {
					FilesResponse response = new FilesResponse(
							client.execute(method));
					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						login();
						method = new HttpPut(storageURL + "/"
								+ sanitizeForURI(container) + "/"
								+ sanitizeForURI(objName));
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
						method.setEntity(entity);
						method.setHeader(entity.getContentType());
						for (String key : metadata.keySet()) {
							method.setHeader(
									FilesConstants.X_OBJECT_META + key,
									sanitizeForURI(metadata.get(key)));
						}
						response = new FilesResponse(client.execute(method));
					}

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						return response.getResponseHeader(FilesConstants.E_TAG)
								.getValue();
					} else {
						logger.debug(response.getStatusLine());
						throw new FilesException("Unexpected result",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * �˷������ڸ���ĳ�����ڵ�ĳ������һ������������Ķ��������档
	 * 
	 * @param sourceContainer
	 *            ���� Դ��������
	 * @param sourceObjName
	 *            ���� Դ��������
	 * @param destContainer
	 *            ���� Ŀ����������
	 * @param destObjName
	 *            ���� Ŀ�Ķ�������
	 * @return ����ɹ��󷵻ظ��ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public String copyObject(String sourceContainer, String sourceObjName,
			String destContainer, String destObjName) throws HttpException,
			IOException {
		String etag = null;
		if (this.isLoggedin()) {

			if (isValidContainerName(sourceContainer)
					&& isValidObjectName(sourceObjName)
					&& isValidContainerName(destContainer)
					&& isValidObjectName(destObjName)) {

				HttpPut method = null;
				try {
					String sourceURI = sanitizeForURI(sourceContainer) + "/"
							+ sanitizeForURI(sourceObjName);
					String destinationURI = sanitizeForURI(destContainer) + "/"
							+ sanitizeForURI(destObjName);

					method = new HttpPut(storageURL + "/" + destinationURI);
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					method.setHeader(FilesConstants.X_COPY_FROM, sourceURI);

					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();

						login();
						method = new HttpPut(storageURL + "/" + destinationURI);
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
						method.setHeader(FilesConstants.X_COPY_FROM, sourceURI);

						response = new FilesResponse(client.execute(method));
					}

					if (response.getStatusCode() == HttpStatus.SC_CREATED) {
						etag = response.getResponseHeader(FilesConstants.E_TAG)
								.getValue();

					} else {
						throw new FilesException(
								"Unexpected status from server",
								response.getResponseHeaders(),
								response.getStatusLine());
					}

				} finally {
					if (method != null) {
						method.abort();
					}
				}
			} else {
				if (!isValidContainerName(sourceContainer)) {
					throw new FilesInvalidNameException(sourceContainer);
				} else if (!isValidObjectName(sourceObjName)) {
					throw new FilesInvalidNameException(sourceObjName);
				} else if (!isValidContainerName(destContainer)) {
					throw new FilesInvalidNameException(destContainer);
				} else {
					throw new FilesInvalidNameException(destObjName);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}

		return etag;
	}

	/**
	 * ɾ��ָ�������ڵĶ���
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param objName
	 *            ���� ָ����������
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesExcepiton
	 *             ���� �洢���������ļ�����
	 */
	public void deleteObject(String container, String objName)
			throws IOException, FilesNotFoundException, HttpException,
			FilesException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				HttpDelete method = null;
				try {
					method = new HttpDelete(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(objName));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						login();
						method = new HttpDelete(storageURL + "/"
								+ sanitizeForURI(container) + "/"
								+ sanitizeForURI(objName));
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
						response = new FilesResponse(client.execute(method));
					}

					if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
						logger.debug("Object Deleted : " + objName);
					} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						throw new FilesNotFoundException(
								"Object was not found " + objName,
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException(
								"Unexpected status from server",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					if (method != null)
						method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * ��ȡָ�������Ԫ���ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param objName
	 *            ���� ָ����������
	 * @return ָ�������Ԫ���ݡ�
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 * @throws FilesInvalidNameException
	 *             ���� ָ��������������������Ϸ���
	 * @throws FilesNotFoundException
	 *             ���� ָ���Ķ��󲻴��ڡ�
	 */
	public FilesObjectMetaData getObjectMetaData(String container,
			String objName) throws IOException, FilesNotFoundException,
			HttpException, FilesAuthorizationException,
			FilesInvalidNameException {
		FilesObjectMetaData metaData;
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				HttpHead method = new HttpHead(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				try {
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
						method.abort();
						login();
						method.getParams().setIntParameter(
								"http.socket.timeout", connectionTimeOut);
						method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
						response = new FilesResponse(client.execute(method));
					}

					if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT
							|| response.getStatusCode() == HttpStatus.SC_OK) {
						logger.debug("Object metadata retreived  : " + objName);
						String mimeType = response.getContentType();
						String lastModified = response.getLastModified();
						String eTag = response.getETag();
						String contentLength = response.getContentLength();

						metaData = new FilesObjectMetaData(mimeType,
								contentLength, eTag, lastModified);

						Header[] headers = response.getResponseHeaders();
						HashMap<String, String> headerMap = new HashMap<String, String>();

						for (Header h : headers) {
							if (h.getName().startsWith(
									FilesConstants.X_OBJECT_META)) {
								headerMap.put(
										h.getName().substring(
												FilesConstants.X_OBJECT_META
														.length()),
										unencodeURI(h.getValue()));
							}
						}
						if (headerMap.size() > 0)
							metaData.setMetaData(headerMap);

						return metaData;
					} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						throw new FilesNotFoundException(
								"Container: " + container
										+ " did not have object " + objName,
								response.getResponseHeaders(),
								response.getStatusLine());
					} else {
						throw new FilesException(
								"Unexpected Return Code from Server",
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
	}

	/**
	 * ��ȡָ����������ݡ�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param objName
	 *            ���� ָ����������
	 * @return ָ�����������
	 * 
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 * @throws FilesInvalidNameException
	 *             ���� ָ��������������������Ϸ���
	 * @throws FilesNotFoundException
	 *             ���� ָ������������󲻴��ڡ�
	 */
	public byte[] getObject(String container, String objName)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesInvalidNameException, FilesNotFoundException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				HttpGet method = new HttpGet(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);

				try {
					FilesResponse response = new FilesResponse(
							client.execute(method));

					if (response.getStatusCode() == HttpStatus.SC_OK) {
						logger.debug("Object data retreived  : " + objName);
						return response.getResponseBody();
					} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						throw new FilesNotFoundException(
								"Container: " + container
										+ " did not have object " + objName,
								response.getResponseHeaders(),
								response.getStatusLine());
					}
				} finally {
					method.abort();
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		return null;
	}

	/**
	 * ��ȡָ����������ݣ������ļ�������ʽ���ء�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param objName
	 *            ���� ָ����������
	 * @return ���ļ�������ʽ����ָ����������ݡ�
	 *         
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 * @throws FilesInvalidNameException
	 *             ���� ָ��������������������Ϸ���
	 * @throws FilesNotFoundException
	 *             ���� ָ������������󲻴��ڡ�
	 */
	public InputStream getObjectAsStream(String container, String objName)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesInvalidNameException, FilesNotFoundException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				if (objName.length() > FilesConstants.OBJECT_NAME_LENGTH) {
					logger.warn("Object Name supplied was truncated to Max allowed of "
							+ FilesConstants.OBJECT_NAME_LENGTH
							+ " characters !");
					objName = objName.substring(0,
							FilesConstants.OBJECT_NAME_LENGTH);
					logger.warn("Truncated Object Name is: " + objName);
				}

				HttpGet method = new HttpGet(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
				FilesResponse response = new FilesResponse(
						client.execute(method));

				if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					method.abort();
					login();
					method = new HttpGet(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(objName));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					response = new FilesResponse(client.execute(method));
				}

				if (response.getStatusCode() == HttpStatus.SC_OK) {
					logger.info("Object data retreived  : " + objName);
					// DO NOT RELEASE THIS CONNECTION
					return response.getResponseBodyAsStream();
				} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
					method.abort();
					throw new FilesNotFoundException("Container: " + container
							+ " did not have object " + objName,
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		return null;
	}

	/**
	 * ��ȡ�ض�����ָ����Χ�ڵ����ݣ������ļ�������ʽ���ء�
	 * 
	 * @param container
	 *            ���� ָ����������
	 * @param objName
	 *            ���� ָ����������
	 * @param offset
	 * 			  ���� ���ݿ�ʼ��λ�á�
	 * @param length
	 *    		  ���� ���ݵĳ��ȡ�
	 * @return ���ļ�������ʽ����ָ����������ݡ�
	 *         
	 * @throws IOException
	 *             ���� ��������ʱ��IO����
	 * @throws HttpException
	 *             ���� HttpЭ�����
	 * @throws FilesAuthorizationException
	 *             ���� �ͻ�����֤���Ϸ���
	 * @throws FilesInvalidNameException
	 *             ���� ָ��������������������Ϸ���
	 * @throws FilesNotFoundException
	 *             ���� ָ������������󲻴��ڡ�
	 */
	public InputStream getObjectAsRangedStream(String container,
			String objName, long offset, long length) throws IOException,
			HttpException, FilesAuthorizationException,
			FilesInvalidNameException, FilesNotFoundException {
		if (this.isLoggedin()) {
			if (isValidContainerName(container) && isValidObjectName(objName)) {
				if (objName.length() > FilesConstants.OBJECT_NAME_LENGTH) {
					logger.warn("Object Name supplied was truncated to Max allowed of "
							+ FilesConstants.OBJECT_NAME_LENGTH
							+ " characters !");
					objName = objName.substring(0,
							FilesConstants.OBJECT_NAME_LENGTH);
					logger.warn("Truncated Object Name is: " + objName);
				}

				HttpGet method = new HttpGet(storageURL + "/"
						+ sanitizeForURI(container) + "/"
						+ sanitizeForURI(objName));
				method.getParams().setIntParameter("http.socket.timeout",
						connectionTimeOut);
				method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
				if (offset >= 0) {
					method.setHeader("Range", "bytes=" + offset + "-" + length);
				} else {
					method.setHeader("Range", "bytes=" + offset + "-");
				}
				FilesResponse response = new FilesResponse(
						client.execute(method));

				if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
					method.abort();
					login();
					method = new HttpGet(storageURL + "/"
							+ sanitizeForURI(container) + "/"
							+ sanitizeForURI(objName));
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					response = new FilesResponse(client.execute(method));
				}

				if (response.getStatusCode() == HttpStatus.SC_OK) {
					logger.info("Object data retreived  : " + objName);
					// DO NOT RELEASE THIS CONNECTION
					return response.getResponseBodyAsStream();
				} else if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
					method.abort();
					throw new FilesNotFoundException("Container: " + container
							+ " did not have object " + objName,
							response.getResponseHeaders(),
							response.getStatusLine());
				}
			} else {
				if (!isValidObjectName(objName)) {
					throw new FilesInvalidNameException(objName);
				} else {
					throw new FilesInvalidNameException(container);
				}
			}
		} else {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		return null;
	}

	/**
	 * ��������д�뵽�ļ��С�
	 * 
	 * @param is
	 * 			���� ������
	 * @param f
	 * 			���� �����ļ�
	 * 
	 * @throws IOException
	 * 			���� ��������ʱ��IO����
	 */
	static void writeInputStreamToFile(InputStream is, File f)
			throws IOException {
		BufferedOutputStream bf = new BufferedOutputStream(
				new FileOutputStream(f));
		byte[] buffer = new byte[1024];
		int read = 0;

		while ((read = is.read(buffer)) > 0) {
			bf.write(buffer, 0, read);
		}

		is.close();
		bf.flush();
		bf.close();
	}

	/**
	 * ��������ת��Ϊָ�������ʽ���ַ�����
	 * 
	 * @param stream
	 * 			���� �ض���������
	 * @param encoding
	 * 			���� ָ�������ʽ��
	 * @return ���ɵ��ַ�����
	 * 
	 * @throws IOException
	 * 			���� ��������ʱ��IO����
	 */
	static String inputStreamToString(InputStream stream, String encoding)
			throws IOException {
		char buffer[] = new char[4096];
		StringBuilder sb = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(stream, "utf-8"); // For
																		// now,
																		// assume
																		// utf-8
																		// to
																		// work
																		// around
																		// server
																		// bug

		int nRead = 0;
		while ((nRead = isr.read(buffer)) >= 0) {
			sb.append(buffer, 0, nRead);
		}
		isr.close();

		return sb.toString();
	}

	/**
	 * ����ĳ���ļ���Md5ֵ��
	 * 
	 * @param f
	 * 			���� ָ���ļ�
	 * @return ���ļ���MD5ֵ��
	 * 
	 * @throws IOException
	 * 			���� ��������ʱ��IO���� 
	 */
	public static String md5Sum(File f) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			InputStream is = new FileInputStream(f);
			byte[] buffer = new byte[1024];
			int read = 0;

			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}

			is.close();

			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);

			// Front load any zeros cut off by BigInteger
			String md5 = bigInt.toString(16);
			while (md5.length() != 32) {
				md5 = "0" + md5;
			}
			return md5;
		} catch (NoSuchAlgorithmException e) {
			logger.fatal("The JRE is misconfigured on this computer", e);
			return null;
		}
	}

	/**
	 * ����ĳ�ֽ������Md5ֵ��
	 * 
	 * @param data
	 * 			���� ָ���ֽ�����
	 * @return ���ֽ������MD5ֵ��
	 * 
	 * @throws IOException
	 * 			���� ��������ʱ��IO���� 
	 */
	public static String md5Sum(byte[] data) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] md5sum = digest.digest(data);
			BigInteger bigInt = new BigInteger(1, md5sum);

			// Front load any zeros cut off by BigInteger
			String md5 = bigInt.toString(16);
			while (md5.length() != 32) {
				md5 = "0" + md5;
			}
			return md5;
		} catch (NoSuchAlgorithmException nsae) {
			logger.fatal("Major problems with your Java configuration", nsae);
			return null;
		}

	}

	/**
	 * ��ָ���ַ���ת��Ϊ��׼URI��ʽ��
	 * @param str
	 * 			���� ָ���ַ�����
	 * @return ��׼��URI��ʽ�ַ�����
	 */
	public static String sanitizeForURI(String str) {
		URLCodec codec = new URLCodec();
		try {
			return codec.encode(str).replaceAll("\\+", "%20");
		} catch (EncoderException ee) {
			logger.warn("Error trying to encode string for URI", ee);
			return str;
		}
	}
	/**
	 * ��ָ���ַ���ת��Ϊ��׼URI��ʽ����������/�����š�
	 * @param str
	 * 			���� ָ���ַ�����
	 * @return ��׼��URI��ʽ�ַ�����
	 */
	public static String sanitizeAndPreserveSlashes(String str) {
		URLCodec codec = new URLCodec();
		try {
			return codec.encode(str).replaceAll("\\+", "%20")
					.replaceAll("%2F", "/");
		} catch (EncoderException ee) {
			logger.warn("Error trying to encode string for URI", ee);
			return str;
		}
	}

	/**
	 * ����׼URI��ʽ���롣
	 * @param str
	 * 			���� ��׼URI��ʽ�ַ�����
	 * @return �������ַ�����
	 */
	public static String unencodeURI(String str) {
		URLCodec codec = new URLCodec();
		try {
			return codec.decode(str);
		} catch (DecoderException ee) {
			logger.warn("Error trying to encode string for URI", ee);
			return str;
		}

	}
	
	/**
	 * ��ȡ�ͻ��˷��ʷ�����������ʱ�ޡ�
	 * 
	 * @return �ͻ��˷��ʷ�����������ʱ��
	 */
	public int getConnectionTimeOut() {
		return connectionTimeOut;
	}
	
	/**
	 * �趨�ͻ��˷��ʷ�����������ʱ�ޡ�
	 * 
	 * @param connectionTimeOut
	 * 			���� ����ʱ�޵���ֵ��
	 */
	public void setConnectionTimeOut(int connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}
	
	/**
	 * ��ȡ���������صĴ洢��ַ��
	 * 
	 * @return ���������صĴ洢��ַ��
	 */
	public String getStorageURL() {
		return storageURL;
	}
	
	/**
	 * ��ȡ���������ص���Ȩ���ơ�
	 * 
	 * @return ���������ص���Ȩ���ơ�
	 */
	public String getAuthToken() {
		return authToken;
	}
	
	/**
	 * �жϿͻ����Ƿ��ѵ�¼��
	 * 
	 * @return ��¼�ɹ������棬����Ϊ�١�
	 */
	public boolean isLoggedin() {
		return isLoggedin;
	}

	/**
	 * ��ȡ�ͻ��˵�¼���û�����
	 * 
	 * @return �ͻ��˵�¼���û�����
	 */
	public String getUserName() {
		return username;
	}
	
	/**
	 * �趨�ͻ��˵�¼���û�����
	 */
	public void setUserName(String userName) {
		this.username = userName;
	}

	/**
	 * ��ȡ�ͻ��˵�¼�����롣
	 * 
	 * @return �ͻ��˵�¼�����롣
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * �趨�ͻ��˵�¼�����롣
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * ��ȡ���������ص���֤��ַ��
	 * 
	 * @return ���������ص���֤��ַ��
	 */
	public String getAuthenticationURL() {
		return authenticationURL;
	}

	/**
	 * �趨���������ص���֤��ַ��
	 */
	public void setAuthenticationURL(String authenticationURL) {
		this.authenticationURL = authenticationURL;
	}

//	public boolean getUseETag() {
//		return useETag;
//	}
//
//	public void setUseETag(boolean useETag) {
//		this.useETag = useETag;
//	}

	private void setUserAgent(String userAgent) {
		client.getParams().setParameter(HTTP.USER_AGENT, userAgent);
	}

	private String getUserAgent() {
		return client.getParams().getParameter(HTTP.USER_AGENT).toString();
	}

	private static boolean isValidContainerName(String name) {
		if (name == null)
			return false;
		int length = name.length();
		if (length == 0 || length > FilesConstants.CONTAINER_NAME_LENGTH)
			return false;
		if (name.indexOf('/') != -1)
			return false;
		// if (name.indexOf('?') != -1) return false;
		return true;
	}

	private static boolean isValidObjectName(String name) {
		if (name == null)
			return false;
		int length = name.length();
		if (length == 0 || length > FilesConstants.OBJECT_NAME_LENGTH)
			return false;
		// if (name.indexOf('?') != -1) return false;
		return true;
	}

	private boolean updateObjectManifest(String container, String object,
			String manifest) throws FilesAuthorizationException, HttpException,
			IOException, FilesInvalidNameException {
		return updateObjectMetadataAndManifest(container, object,
				new HashMap<String, String>(), manifest);
	}

	private boolean updateObjectMetadata(String container, String object,
			Map<String, String> metadata) throws FilesAuthorizationException,
			HttpException, IOException, FilesInvalidNameException {
		return updateObjectMetadataAndManifest(container, object, metadata,
				null);
	}

	private boolean updateObjectMetadataAndManifest(String container,
			String object, Map<String, String> metadata, String manifest)
			throws FilesAuthorizationException, HttpException, IOException,
			FilesInvalidNameException {
		FilesResponse response;

		if (!isLoggedin) {
			throw new FilesAuthorizationException("You must be logged in",
					null, null);
		}
		if (!isValidContainerName(container))
			throw new FilesInvalidNameException(container);
		if (!isValidObjectName(object))
			throw new FilesInvalidNameException(object);

		String postUrl = storageURL + "/"
				+ FilesClient.sanitizeForURI(container) + "/"
				+ FilesClient.sanitizeForURI(object);

		HttpPost method = null;
		try {
			method = new HttpPost(postUrl);
			if (manifest != null) {
				method.setHeader(FilesConstants.MANIFEST_HEADER, manifest);
			}
			method.getParams().setIntParameter("http.socket.timeout",
					connectionTimeOut);
			method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
			if (!(metadata == null || metadata.isEmpty())) {
				for (String key : metadata.keySet())
					method.setHeader(FilesConstants.X_OBJECT_META + key,
							FilesClient.sanitizeForURI(metadata.get(key)));
			}
			HttpResponse resp = client.execute(method);
			response = new FilesResponse(resp);
			if (response.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				method.abort();
				if (login()) {
					method = new HttpPost(postUrl);
					method.getParams().setIntParameter("http.socket.timeout",
							connectionTimeOut);
					method.setHeader(FilesConstants.X_AUTH_TOKEN, authToken);
					if (!(metadata == null || metadata.isEmpty())) {
						for (String key : metadata.keySet())
							method.setHeader(
									FilesConstants.X_OBJECT_META + key,
									FilesClient.sanitizeForURI(metadata
											.get(key)));
					}
					client.execute(method);
				}
			}

			return true;
		} finally {
			if (method != null)
				method.abort();
		}

	}

	private String makeURI(String base, List<NameValuePair> parameters) {
		return base + "?" + URLEncodedUtils.format(parameters, "UTF-8");
	}

	private void useSnet() {
		if (snet) {
		} else {
			snet = true;
			if (storageURL != null) {
				storageURL = snetAddr + storageURL.substring(8);
			}
		}
	}

	private void usePublic() {
		if (!snet) {
		} else {
			snet = false;
			if (storageURL != null) {
				storageURL = "https://"
						+ storageURL.substring(snetAddr.length());
			}
		}
	}

	private boolean usingSnet() {
		return snet;
	}

	private boolean envSnet() {
		if (System.getenv("RACKSPACE_SERVICENET") == null) {
			return false;
		} else {
			snet = true;
			return true;
		}
	}
}
