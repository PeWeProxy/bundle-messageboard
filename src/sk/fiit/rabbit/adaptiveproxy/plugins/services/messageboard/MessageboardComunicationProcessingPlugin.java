package sk.fiit.rabbit.adaptiveproxy.plugins.services.messageboard;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.headers.ResponseHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.PageInformationProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;



public class MessageboardComunicationProcessingPlugin  extends JavaScriptInjectingProcessingPlugin {
	
	private String defaultLanguage;
	private String defaultVisibility;
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		StringContentService stringContentService = request.getServicesHandle().getService(StringContentService.class);

		Map<String, String> postData = getPostDataFromRequest(stringContentService.getContent());
		String content = "";
		Connection connection = null;
		try {
			connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();

			if (request.getRequestHeader().getRequestURI().contains("action=setMessageboardNick")) {
				content = this.setMessageboardNick(connection, postData.get("uid"), postData.get("nick"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=getMessages")) {
				content = this.getMessages(connection, Integer.parseInt(postData.get("from")), Integer.parseInt(postData.get("count")), Boolean.parseBoolean(postData.get("decorateLinks")), request.getRequestHeader().getField("Referer"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=messageboardNickExists")) {
				content = this.messageboardNickExists(connection, postData.get("uid"), postData.get("nick")).toString();
			}
			if (request.getRequestHeader().getRequestURI().contains("action=addMessage")) {
				content = this.addMessage(connection, postData.get("uid"), postData.get("nick"), postData.get("text"), request.getRequestHeader().getField("Referer"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=getUserPreferences")) {
				content = this.getUserPreferences(connection, postData.get("uid")).toJSONString();
			}
			if (request.getRequestHeader().getRequestURI().contains("action=getMessageCount")) {
				content = this.getMessageCount(connection, request.getRequestHeader().getField("Referer")) + "";
			}
			if (request.getRequestHeader().getRequestURI().contains("action=setShown")) {
				content = this.setShown(connection, postData.get("uid"), postData.get("shown"));
			}
			
		} finally {
			SqlUtils.close(connection);
		}
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	private String setShown (Connection connection, String uid, String shown) {
		PreparedStatement stmt = null;
		
		try {
			stmt = connection.prepareStatement("UPDATE `messageboard_user_preferences` SET `visibility` = ? WHERE `userid` = ?;");
			stmt.setInt(1, Integer.parseInt(shown));
			stmt.setString(2, uid);

			stmt.execute();
			
			return "OK";
		} catch (SQLException e) {
			logger.error("Could not get messageboard count ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(stmt);
		}
	}
	
	private int getMessageCount (Connection connection, String url) {
		PreparedStatement stmt = null;
		int messageCount = 0;
		
		try {
			stmt = connection.prepareStatement("SELECT COUNT(*) FROM `messageboard_messages` WHERE `url` = ?;");
			stmt.setString(1, url);

			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			
			while (rs.next()) {
				messageCount = rs.getInt(1);
			}
			
		} catch (SQLException e) {
			logger.error("Could not get messageboard count ", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		return messageCount;
	}
	
	private JSONObject getUserPreferences (Connection connection, String uid) {
		PreparedStatement stmt = null;
		JSONObject userPreferences = new JSONObject();
		
		try {
			stmt = connection.prepareStatement("SELECT * FROM `messageboard_user_preferences` WHERE `userid` = ? LIMIT 1;");
			stmt.setString(1, uid);

			stmt.execute();
			ResultSet rs = stmt.getResultSet();

			if (rs.next()) {
				userPreferences.put("messageboard_nick", rs.getString(3));
				userPreferences.put("language", rs.getString(4));
				userPreferences.put("visibility", rs.getString(5));
				
			} else {
				stmt = connection.prepareStatement("INSERT INTO `messageboard_user_preferences` (`userid`, `language`, `visibility`) VALUES (?, ?, ?);");
				stmt.setString(1, uid);
				stmt.setString(2, this.defaultLanguage);
				stmt.setString(3, this.defaultVisibility);
				
				stmt.execute();
				
				userPreferences.put("messageboard_nick", "");
				userPreferences.put("language", this.defaultLanguage);
				userPreferences.put("visibility", this.defaultVisibility);
			}
			
		} catch (SQLException e) {
			logger.error("Could select messageboard nick count ", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		
		return userPreferences;
	}
	
	private String setMessageboardNick (Connection connection, String uid, String nick) {
		PreparedStatement nickExists_stmt = null;
		PreparedStatement updateNick_stmt = null;
		
		try {
			int rowCount = 0;
			nickExists_stmt = connection.prepareStatement("SELECT COUNT(*) FROM `messageboard_user_preferences` WHERE `messageboard_nick` = ? LIMIT 1;");
			nickExists_stmt.setString(1, nick);

			nickExists_stmt.execute();
			ResultSet rs = nickExists_stmt.getResultSet();

			while (rs.next()) {
				rowCount = rs.getInt(1);
			}
			if (rowCount > 0) {
				return "NICK_EXISTS";
			}
			
			try {
				updateNick_stmt = connection.prepareStatement("UPDATE `messageboard_user_preferences` SET `messageboard_nick` = ? WHERE `userid` = ? LIMIT 1;");
				updateNick_stmt.setString(1, nick);
				updateNick_stmt.setString(2, uid);

				updateNick_stmt.execute();
			} catch (SQLException e) {
				logger.error("Could select messageboard nick count ", e);
				return "FAIL";
			} finally {
				SqlUtils.close(updateNick_stmt);
			}
			
			return "OK";
			
		} catch (SQLException e) {
			logger.error("Could select messageboard nick count ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(nickExists_stmt);
		}
	}

	private String getMessages (Connection connection, int from, int count, boolean decorateLinks, String url) {
		PreparedStatement stmt = null;
		int messageCount = 0;
		String formatedTime = "";
		JSONObject messageList = new JSONObject();
		
		LinkedList l1 = new LinkedList();
		LinkedHashMap m1;
		
		try {
			stmt = connection.prepareStatement("SELECT `messageboard_nick`, `timestamp`, `text`, `url` FROM `messageboard_messages` LEFT JOIN `messageboard_user_preferences` ON `messageboard_messages`.`userid` = `messageboard_user_preferences`.`userid` WHERE `url` = ? ORDER BY `messageboard_messages`.`id` DESC LIMIT ?, ?;");
			stmt.setString(1, url);
			stmt.setInt(2, from);
			stmt.setInt(3, count);
			
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			rs.last();
			
			do {
				m1 = new LinkedHashMap();
				m1.put("text", decorateLinks(rs.getString(3)));
				m1.put("nick", rs.getString(1));
				m1.put("time", formatDatetime(rs.getString(2)));
				l1.add(m1);
			} while (rs.previous());
		} catch (SQLException e) {
			logger.error("Could select messages ", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		try {
			stmt = connection.prepareStatement("SELECT COUNT(*) FROM `messageboard_messages` WHERE `url` = ?;");
			stmt.setString(1, url);

			stmt.execute();
			ResultSet rs = stmt.getResultSet();

			while (rs.next()) {
				messageCount = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("Could get message count ", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		messageList.put("messages", l1);
		messageList.put("total", messageCount);
		
		String jsonString = messageList.toJSONString();
		return jsonString;
	}
	
	private Boolean messageboardNickExists (Connection connection, String uid, String nick) {
		PreparedStatement stmt = null;
		int rowCount = 1;
		
		try {
			stmt = connection.prepareStatement("SELECT COUNT(*) FROM `messageboard_user_preferences` WHERE `messageboard_nick` = ? LIMIT 1;");
			stmt.setString(1, nick);

			stmt.execute();
			ResultSet rs = stmt.getResultSet();

			while (rs.next()) {
				rowCount = rs.getInt(1);
			}
			
		} catch (SQLException e) {
			logger.error("Could select messageboard nick count ", e);
		} finally {
			SqlUtils.close(stmt);
		}
		
		if (rowCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	private String addMessage (Connection connection, String uid, String nick, String messageText, String url) {
		PreparedStatement stmt = null;
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0, timestamp.indexOf("."));
		
		messageText = messageText.replaceAll("\\<.*?>","");
		
		try {
			stmt = connection.prepareStatement("INSERT INTO `messageboard_messages` (`userid`, `timestamp`, `url`, `text`) VALUES (?, ?, ?, ?);");
			stmt.setString(1, uid);
			stmt.setString(2, formatedTimeStamp);
			stmt.setString(3, url);
			stmt.setString(4, messageText);

			stmt.execute();
			
			return "OK";
		} catch (SQLException e) {
			logger.error("Could select messageboard nick count ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(stmt);
		}
	}
	
	private Map<String, String> getPostDataFromRequest (String requestContent) {
		try {
			requestContent = URLDecoder.decode(requestContent, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.warn(e);
		}
		Map<String, String> postData = new HashMap<String, String>();
		String attributeName;
		String attributeValue;

		for (String postPair : requestContent.split("&")) {
			if (postPair.split("=").length == 2) {
				attributeName = postPair.split("=")[0];
				attributeValue = postPair.split("=")[1];
				postData.put(attributeName, attributeValue);
			}
		}

		return postData;
	}
	
	private String decorateLinks(String input) {
		String linkRegex = "(((http://|https://|ftp://)|(www.))+(([a-zA-Z0-9\\.-]+\\.[a-zA-Z]{2,4})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(/[a-zA-Z0-9%:/-_\\?\\.'~]*)?)*";
		Pattern linkPattern = Pattern.compile(linkRegex);
		Matcher linkMatcher = linkPattern.matcher(input);
		
		List<String> links = new ArrayList<String>();
		
		while (linkMatcher.find()) {
			if (!"".equals(linkMatcher.group())) {
				links.add(linkMatcher.group());
			}
		}

		String href;
		for (String link : links){
			href = link;
			if (!href.startsWith("http://")){
				href = "http://"+href;
			}

			input = input.replaceFirst(link, "<a href=\""+href+"\">"+link+"</a>");
		}

		return input;
	}
	
	private String formatDatetime (String datetime) {
		String formatedDatetime = "";
		String[] parts_1 = datetime.split(" ");
		String[] parts_2 = parts_1[0].split("-");
		
		formatedDatetime = parts_2[2] + "." + parts_2[1] + "." + parts_2[0] + " " + parts_1[1].substring(0, parts_1[1].length() - 5);
		
		return formatedDatetime;
	}
		
	@Override
	public boolean start (PluginProperties props) {
		super.start(props);
		this.defaultLanguage = props.getProperty("defaultLanguage", "sk");
		this.defaultVisibility = props.getProperty("defaultVisibility", "0");
		return true;
	}

	@Override
	public void desiredRequestServices (
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
		desiredServices.add(ModifiableStringService.class); //FIXME: toto je docasny hack kvoli late processingu, spravne tu ma byt len StringContentService
		desiredServices.add(DatabaseConnectionProviderService.class);
	}
	
	@Override
	public void desiredResponseServices (
			Set<Class<? extends ProxyService>> desiredServices,
			ResponseHeader webRPHeader) {
		super.desiredResponseServices(desiredServices, webRPHeader);
		desiredServices.add(PageInformationProviderService.class);
	}
}
