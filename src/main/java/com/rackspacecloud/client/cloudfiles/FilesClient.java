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
 * 存储系统客户端。这里给出了客户端登录、创建容器或对象、获取对象以及删除容器或对象等基本操作。
 * 在Sample文件夹下，给出了一个更为详细的测试用例，敬请查阅。
 * 
 * <pre>
 * 
 *  // 采用常规方式构造客户端，用户名："jdoe"，密码："johnsdogsname"。 
 * 	FilesClient myClient = FilesClient("jdoe", "johnsdogsname");
 * 
 * 
 *  // 登录过程 (<code>login()</code>），登录失败时返回false。
 *  assert(myClient.login());
 * 
 *  // 确定账户内不存在容器。
 *  assert(myClient.listContainers.length() == 0);
 *  
 *  // 创建一个新容器。
 *  assert(myClient.createContainer("myContainer"));
 *  
 *  // 查看创建后的容器。
 *  assert(myClient.listContainers.length() == 1);
 *  
 *  // 上传文件 "alpaca.jpg"
 *  assert(myClient.storeObject("myContainer", new File("alapca.jpg"), "image/jpeg"));
 *  
 *  // 下载文件 "alpaca.jpg"
 *  FilesObject obj = myClient.getObject("myContainer", "alpaca.jpg");
 *  byte data[] = obj.getObject();
 *  
 *  // 清楚上述操作产生的记录。
 *  // 注：删除操作的顺序要注意，必须在清空容器内的所有对象以后才能删除容器。
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
	 * 构造函数
	 * 
	 * @param client
	 *            —— 访问存储系统的HttpClient。
	 * @param username
	 *            —— 登录所用的用户名。
	 * @param password
	 *            —— 登录所用的密码。
	 * @param authUrl
	 *            —— 存储系统访问地址
	 * @param account
	 *            —— 存储系统中的账户。
	 * @param connectionTimeOut
	 *            —— 连接超时阈值（ms）。
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
	 * 构造函数
	 * 
	 * @param username
	 *            —— 登录所用的用户名。
	 * @param password
	 *            —— 登录所用的密码。
	 * @param authUrl
	 *            —— 存储系统访问地址。
	 * @param account
	 *            —— 存储系统中的账户。
	 * @param connectionTimeOut
	 *            —— 连接超时阈值（ms）。
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
	 * 构造函数
	 * 
	 * @param username
	 *            —— 登录所用的用户名。
	 * @param password
	 *            —— 登录所用的密码。
	 * @param authUrl
	 *            —— 存储系统访问地址。
	 * 
	 */
	public FilesClient(String username, String password, String authUrl) {
		this(username, password, authUrl, null, FilesUtil
				.getIntProperty("connection_timeout"));
	}

	/**
	 * 构造函数
	 * 
	 * @param username
	 *            —— 登录所用的用户名。
	 * @param password
	 *            —— 登录所用的密码。
	 */
	public FilesClient(String username, String password) {
		this(username, password, null, null, FilesUtil
				.getIntProperty("connection_timeout"));
		// lConnectionManagerogger.warn("LGV");
		// logger.debug("LGV:" + client.getHttpConnectionManager());
	}

	/**
	 * 默认构造函数，参数全部通过cloudfiles.properties文件中的设定值得到
	 * 
	 */
	public FilesClient() {
		this(FilesUtil.getProperty("username"), FilesUtil
				.getProperty("password"), null, FilesUtil
				.getProperty("account"), FilesUtil
				.getIntProperty("connection_timeout"));
	}

	/**
	 * 返回认证后访问地址中含有的账户名。
	 * 
	 * @return The account name
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * 设定认证的账户和访问地址。
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
	 * 登录方法：负责验证用户的合法性。
	 * 
	 * @return 登陆成功返回真，否则返回假。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
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
	 * 构造认证系统所需信息的JSON格式。
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
	 * 登录方法：负责验证用户的合法性。
	 * 
	 * @return 登陆成功返回真，否则返回假。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
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
	 * 列出容器内的所有容器信息，并排序显示。
	 * 
	 * @return 返回用户指定账户内的容器信息列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesContainerInfo> listContainersInfo() throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainersInfo(-1, null);
	}

	/**
	 * 列出容器内的所有容器信息，并排序显示。
	 * 
	 * @param limit
	 *            —— 返回容器信息列表的最大长度。
	 * 
	 * @return 返回用户指定账户内的容器信息列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesContainerInfo> listContainersInfo(int limit)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listContainersInfo(limit, null);
	}

	/**
	 * 列出容器内的所有容器信息，并排序显示。
	 * 
	 * @param limit
	 *            —— 返回容器信息列表的最大长度。
	 * @param marker
	 *            —— 返回正常排序时指定容器名之后的容器信息列表。
	 * 
	 * @return 返回用户指定账户内的容器信息列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 列出容器内的所有容器名，并排序显示。
	 * 
	 * @return 返回用户指定账户内的容器名列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesContainer> listContainers() throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainers(-1, null);
	}

	/**
	 * 列出容器内的所有容器名，并排序显示。
	 * 
	 * @param limit
	 *            —— 返回容器信息列表的最大长度。
	 * 
	 * @return 返回用户指定账户内的容器名列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesContainer> listContainers(int limit) throws IOException,
			HttpException, FilesAuthorizationException, FilesException {
		return listContainers(limit, null);
	}

	/**
	 * 列出容器内的所有容器名，并排序显示。
	 * 
	 * @param limit
	 *            —— 返回容器信息列表的最大长度。
	 * @param marker
	 *            —— 返回正常排序时指定容器名之后的容器信息列表。
	 * 
	 * @return 返回用户指定账户内的容器名列表，如果账户内为空，返回列表长度为0。 用户未登录或指定的账户不存在时均返回空。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 列出指定容器内含有特定字符串前缀的所有的对象名列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param startsWith
	 *            —— 特定字符串前缀。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * @param marker
	 *            —— 返回排序后marker指定对象名之后的对象名列表，与limit参数同时使用实现对象名列表的分页处理。
	 * 
	 * @return 指定容器内含有特定字符串前缀的所有的对象名列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjectsStartingWith(String container,
			String startsWith, String path, int limit, String marker)
			throws IOException, FilesException {
		return listObjectsStartingWith(container, startsWith, path, limit,
				marker, null);
	}

	/**
	 * 列出指定容器内含有特定字符串前缀的所有的对象名列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param startsWith
	 *            —— 特定字符串前缀。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * @param marker
	 *            —— 返回排序后marker指定对象名之后的对象名列表，与limit参数同时使用实现对象名列表的分页处理。
	 * @param delimter
	 *            —— 指定特定的分隔符，主要用来实现伪造的目录结构。
	 * 
	 * @return A list of FilesObjects starting with the given string
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container) throws IOException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, -1, null, null);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param delimter
	 *            —— 指定特定的分隔符，主要用来实现伪造的目录结构。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, Character delimiter)
			throws IOException, FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, -1, null,
				delimiter);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, int limit)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listObjectsStartingWith(container, null, null, limit, null, null);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, String path)
			throws IOException, HttpException, FilesAuthorizationException,
			FilesException {
		return listObjectsStartingWith(container, null, path, -1, null, null);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * @param delimter
	 *            —— 指定特定的分隔符，主要用来实现伪造的目录结构。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, String path,
			Character delimiter) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, -1, null,
				delimiter);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, String path,
			int limit) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, limit, null);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 只列出路径下的对象列表。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * @param marker
	 *            —— 返回排序后marker指定对象名之后的对象名列表，与limit参数同时使用实现对象名列表的分页处理。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, String path,
			int limit, String marker) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, path, limit, marker);
	}

	/**
	 * 列出指定容器内的对象名排序列表。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param limit
	 *            —— 限定对象列表的最大长度。
	 * @param marker
	 *            —— 返回排序后marker指定对象名之后的对象名列表，与limit参数同时使用实现对象名列表的分页处理。
	 * 
	 * @return 指定容器内的对象名排序列表。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 */
	public List<FilesObject> listObjects(String container, int limit,
			String marker) throws IOException, HttpException,
			FilesAuthorizationException, FilesException {
		return listObjectsStartingWith(container, null, null, limit, marker);
	}

	/**
	 * 判断指定的容器是否存在。
	 * 
	 * @param container
	 *            —— 指定的容器名。
	 * 
	 * @return 存在返回真，否则返回假。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
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
	 * 获取指定账户的详细信息。
	 * 
	 * @return FilesAccountInfo类，其中包含了指定账户中容器的数目和所有文件所占用的字节数。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 获取指定容器的基本信息，包括对象数及该容器的大小。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @return ContainerInfo类，包括容器内对象数目及所有对象占用的空间。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 创建一个容器。
	 * 
	 * @param name
	 *            —— 被创建的容器名。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
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
	 * 删除指定容器。
	 * 
	 * @param name
	 *            —— 指定容器名。
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 * @throws FilesInvalidNameException
	 *             —— 指定的容器名不合法。
	 * @throws FilesNotFoundException
	 *             —— 指定的容器名不存在。
	 * @throws FilesContainerNotEmptyException
	 *             —— 指定的容器不为空。
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
	 * 在容器内生成一个路径，该路径也会被当成一个对象单独存放。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 指定路径全文。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 从给定的路径逐个生成目录路径，该给定的路径会被按照'/'符号分层处理。比如，调用createFullPath("myContainer",
	 * "foo/bar/baz")将会在"myContainer"
	 * 容器下生成"food"、"food/bar"、"foo/bar/baz"三个路径，每一个都是一个单独的对象。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param path
	 *            —— 指定路径全文。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 在服务器端创建对象名单，针对用户上传大于5G的文件时所切分的对象片段，包括对象的元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param manifest
	 *            —— 设定针对对象文件的名单。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public boolean createManifestObject(String container, String contentType,
			String name, String manifest, IFilesTransferCallback callback)
			throws IOException, HttpException, FilesException {
		return createManifestObject(container, contentType, name, manifest,
				new HashMap<String, String>(), callback);
	}

	/**
	 * 在服务器端创建对象名单，针对用户上传大于5G的文件时所切分的对象片段，包括对象的元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param manifest
	 *            —— 设定针对对象文件的名单。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public boolean createManifestObject(String container, String contentType,
			String name, String manifest, Map<String, String> metadata)
			throws IOException, HttpException, FilesException {
		return createManifestObject(container, contentType, name, manifest,
				metadata, null);
	}

	/**
	 * 在服务器端创建对象名单，针对用户上传大于5G的文件时所切分的对象片段，包括对象的元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param manifest
	 *            —— 设定针对对象文件的名单。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 在服务器端保存文件。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * 
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name) throws IOException, HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name,
				new HashMap<String, String>(), null);
	}

	/**
	 * 在服务器端保存文件。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 * 
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name, IFilesTransferCallback callback) throws IOException,
			HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name,
				new HashMap<String, String>(), callback);
	}

	/**
	 * 在服务器端保存文件，包括元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public String storeObjectAs(String container, File obj, String contentType,
			String name, Map<String, String> metadata) throws IOException,
			HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, name, metadata, null);
	}

	/**
	 * 在服务器端保存文件，包括元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 复制对象，并保留对象的原始位置。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public String storeObject(String container, File obj, String contentType)
			throws IOException, HttpException, FilesException {
		return storeObjectAs(container, obj, contentType, obj.getName());
	}

	/**
	 * 在服务器端保存文件，包括元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
	 */
	public boolean storeObject(String container, byte obj[],
			String contentType, String name, Map<String, String> metadata)
			throws IOException, HttpException, FilesException {
		return storeObject(container, obj, contentType, name, metadata, null);
	}

	/**
	 * 在服务器端保存文件，包括元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param obj
	 *            —— 被保存到服务器端的文件。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 在服务器端保存文件，包括元数据，被保存的文件数据来自数据流。 这种方式可以省去文件的长度设置，同时避免了在内存中保存整个文件的副本。
	 * 
	 * @param container
	 *            —— 指定容器名
	 * @param data
	 *            —— 被保存的数据流。
	 * @param contentType
	 *            —— 对象文件的MIME类型。
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @param callback
	 *            —— 设定对象的回调函数，一般为空（NULL）。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 在服务器端保存文件，包括元数据。
	 * 
	 * @param container
	 *            —— 指定容器名
	 * @param name
	 *            —— 对象文件的文件名。
	 * @param entity
	 *            —— 发送保存请求的实体名。
	 * @param metadata
	 *            —— 设定元数据键值对的映射数据。
	 * @param md5sum
	 *            —— 保存数据的MD5值，以32个16进制字符的形式显示。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 此方法用于复制某容器内的某对象到另一容器中以另外的对象名保存。
	 * 
	 * @param sourceContainer
	 *            —— 源容器名。
	 * @param sourceObjName
	 *            —— 源对象名。
	 * @param destContainer
	 *            —— 目的容器名。
	 * @param destObjName
	 *            —— 目的对象名。
	 * @return 保存成功后返回该文件的MD5值。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 删除指定容器内的对象。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param objName
	 *            —— 指定对象名。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesExcepiton
	 *             —— 存储服务器端文件错误。
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
	 * 获取指定对象的元数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param objName
	 *            —— 指定对象名。
	 * @return 指定对象的元数据。
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 * @throws FilesInvalidNameException
	 *             —— 指定的容器名或对象名不合法。
	 * @throws FilesNotFoundException
	 *             —— 指定的对象不存在。
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
	 * 获取指定对象的数据。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param objName
	 *            —— 指定对象名。
	 * @return 指定对象的内容
	 * 
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 * @throws FilesInvalidNameException
	 *             —— 指定的容器名或对象名不合法。
	 * @throws FilesNotFoundException
	 *             —— 指定的容器或对象不存在。
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
	 * 获取指定对象的数据，并以文件流的形式返回。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param objName
	 *            —— 指定对象名。
	 * @return 以文件流的形式返回指定对象的内容。
	 *         
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 * @throws FilesInvalidNameException
	 *             —— 指定的容器名或对象名不合法。
	 * @throws FilesNotFoundException
	 *             —— 指定的容器或对象不存在。
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
	 * 获取特定对象指定范围内的数据，并以文件流的形式返回。
	 * 
	 * @param container
	 *            —— 指定容器名。
	 * @param objName
	 *            —— 指定对象名。
	 * @param offset
	 * 			  —— 数据开始的位置。
	 * @param length
	 *    		  —— 数据的长度。
	 * @return 以文件流的形式返回指定对象的内容。
	 *         
	 * @throws IOException
	 *             —— 网络连接时的IO错误。
	 * @throws HttpException
	 *             —— Http协议错误。
	 * @throws FilesAuthorizationException
	 *             —— 客户端认证不合法。
	 * @throws FilesInvalidNameException
	 *             —— 指定的容器名或对象名不合法。
	 * @throws FilesNotFoundException
	 *             —— 指定的容器或对象不存在。
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
	 * 将数据流写入到文件中。
	 * 
	 * @param is
	 * 			—— 数据流
	 * @param f
	 * 			—— 最终文件
	 * 
	 * @throws IOException
	 * 			—— 网络连接时的IO错误。
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
	 * 将数据流转化为指定编码格式的字符串。
	 * 
	 * @param stream
	 * 			—— 特定数据流。
	 * @param encoding
	 * 			—— 指定编码格式。
	 * @return 生成的字符串。
	 * 
	 * @throws IOException
	 * 			—— 网络连接时的IO错误。
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
	 * 计算某个文件的Md5值。
	 * 
	 * @param f
	 * 			—— 指定文件
	 * @return 该文件的MD5值。
	 * 
	 * @throws IOException
	 * 			—— 网络连接时的IO错误。 
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
	 * 计算某字节数组的Md5值。
	 * 
	 * @param data
	 * 			—— 指定字节数组
	 * @return 该字节数组的MD5值。
	 * 
	 * @throws IOException
	 * 			—— 网络连接时的IO错误。 
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
	 * 将指定字符串转化为标准URI格式。
	 * @param str
	 * 			—— 指定字符串。
	 * @return 标准的URI格式字符串。
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
	 * 将指定字符串转化为标准URI格式，并保留‘/’符号。
	 * @param str
	 * 			—— 指定字符串。
	 * @return 标准的URI格式字符串。
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
	 * 将标准URI格式解码。
	 * @param str
	 * 			—— 标准URI格式字符串。
	 * @return 解码后的字符串。
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
	 * 获取客户端访问服务器的连接时限。
	 * 
	 * @return 客户端访问服务器的连接时限
	 */
	public int getConnectionTimeOut() {
		return connectionTimeOut;
	}
	
	/**
	 * 设定客户端访问服务器的连接时限。
	 * 
	 * @param connectionTimeOut
	 * 			—— 连接时限的数值。
	 */
	public void setConnectionTimeOut(int connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}
	
	/**
	 * 获取服务器返回的存储地址。
	 * 
	 * @return 服务器返回的存储地址。
	 */
	public String getStorageURL() {
		return storageURL;
	}
	
	/**
	 * 获取服务器返回的授权令牌。
	 * 
	 * @return 服务器返回的授权令牌。
	 */
	public String getAuthToken() {
		return authToken;
	}
	
	/**
	 * 判断客户端是否已登录。
	 * 
	 * @return 登录成功返回真，否则为假。
	 */
	public boolean isLoggedin() {
		return isLoggedin;
	}

	/**
	 * 获取客户端登录的用户名。
	 * 
	 * @return 客户端登录的用户名。
	 */
	public String getUserName() {
		return username;
	}
	
	/**
	 * 设定客户端登录的用户名。
	 */
	public void setUserName(String userName) {
		this.username = userName;
	}

	/**
	 * 获取客户端登录的密码。
	 * 
	 * @return 客户端登录的密码。
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * 设定客户端登录的密码。
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 获取服务器返回的认证地址。
	 * 
	 * @return 服务器返回的认证地址。
	 */
	public String getAuthenticationURL() {
		return authenticationURL;
	}

	/**
	 * 设定服务器返回的认证地址。
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
