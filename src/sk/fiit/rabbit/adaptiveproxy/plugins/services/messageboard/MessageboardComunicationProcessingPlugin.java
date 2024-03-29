package sk.fiit.rabbit.adaptiveproxy.plugins.services.messageboard;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.svenson.JSON;

import sk.fiit.peweproxy.headers.RequestHeader;
import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.peweproxy.services.ProxyService;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.RequestDataParserService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserIdentificationService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.UserPreferencesProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.bubble.BubbleMenuProcessingPlugin;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.JdbcTemplate.ResultProcessor;
import sk.fiit.rabbit.adaptiveproxy.plugins.utils.SqlUtils;

public class MessageboardComunicationProcessingPlugin extends BubbleMenuProcessingPlugin {
	
	private String defaultLanguage;
	private String defaultVisibility;
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		String content = "";
		
		if(request.getServicesHandle().isServiceAvailable(RequestDataParserService.class) &&
				request.getServicesHandle().isServiceAvailable(UserPreferencesProviderService.class) &&
				request.getServicesHandle().isServiceAvailable(UserIdentificationService.class)) {
			Map<String, String> postData = request.getServicesHandle().getService(RequestDataParserService.class).getDataFromPOST();
			
			Connection connection = null;
			
			UserPreferencesProviderService userPreferencesProvider = request.getServicesHandle().getService(UserPreferencesProviderService.class);
			String userId = request.getServicesHandle().getService(UserIdentificationService.class).getClientIdentification();
			
			if(postData != null && request.getServicesHandle().isServiceAvailable(DatabaseConnectionProviderService.class)) {				
				try {
					
					connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();
					JdbcTemplate jdbc = new JdbcTemplate(connection);
		
					if (request.getRequestHeader().getRequestURI().contains("action=setMessageboardNick")) {
						content = this.setMessageboardNick(jdbc, userPreferencesProvider, postData.get("uid"), postData.get("nick"));
					}
					if (request.getRequestHeader().getRequestURI().contains("action=getMessages")) {
						content = this.getMessages(jdbc, userPreferencesProvider, userId, Integer.parseInt(postData.get("from")), Boolean.parseBoolean(postData.get("decorateLinks")), request.getRequestHeader().getField("Referer"));
					}
					if (request.getRequestHeader().getRequestURI().contains("action=messageboardNickExists")) {
						content = this.messageboardNickExists(jdbc, postData.get("nick")).toString();
					}
					if (request.getRequestHeader().getRequestURI().contains("action=addMessage")) {
						content = this.addMessage(jdbc, postData.get("uid"), postData.get("nick"), postData.get("text"), request.getRequestHeader().getField("Referer"));
					}
					if (request.getRequestHeader().getRequestURI().contains("action=getUserPreferences")) {
						content = this.getUserPreferences(jdbc, userPreferencesProvider, postData.get("uid"));
					}
					if (request.getRequestHeader().getRequestURI().contains("action=getMessageCount")) {
						content = this.getMessageCount(jdbc, request.getRequestHeader().getField("Referer")) + "";
					}
					if (request.getRequestHeader().getRequestURI().contains("action=setShown")) {
						content = this.setShown(jdbc, postData.get("uid"), postData.get("shown"));
					}
					
				} finally {
					SqlUtils.close(connection);
				}
			}
		}
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	private String setShown (JdbcTemplate jdbc, String uid, String shown) {
		jdbc.update("UPDATE messageboard_user_preferences SET visibility = ? WHERE userid = ?", new Object[] { shown, uid });
		return "OK";
	}
	
	private int getMessageCount (JdbcTemplate jdbc, String url) {
		return jdbc.queryFor("SELECT COUNT(*) FROM messageboard_messages WHERE url = ?", new Object[] { url }, Integer.class);
	}
	
	@SuppressWarnings("unchecked")
	private String getUserPreferences (JdbcTemplate jdbc, final UserPreferencesProviderService userPreferencesProvider, final String uid) {
		LinkedHashMap userPreferences = 
			jdbc.find("SELECT messageboard_nick, language, visibility FROM messageboard_user_preferences WHERE userid = ? LIMIT 1", 
				new Object[] { uid }, 
				new ResultProcessor<LinkedHashMap>() {
			@Override
			public LinkedHashMap processRow(ResultSet rs) throws SQLException {
				LinkedHashMap userPreferences = new LinkedHashMap();
//				JSONObject userPreferences = new JSONObject();
				userPreferences.put("messageboard_nick", userPreferencesProvider.getProperty("nick", uid, "messageboard"));
				userPreferences.put("language", rs.getString("language"));
				userPreferences.put("visibility", rs.getString("visibility"));
				return userPreferences;
			}
		});
		
		if(userPreferences == null) {
			jdbc.insert("INSERT INTO messageboard_user_preferences (userid, language, visibility) VALUES (?, ?, ?)", 
					new Object[] { uid, this.defaultLanguage, this.defaultVisibility } );
			userPreferences = new LinkedHashMap();
			userPreferences.put("messageboard_nick", "");
			userPreferences.put("language", this.defaultLanguage);
			userPreferences.put("visibility", this.defaultVisibility);
		}
		
		return JSON.defaultJSON().forValue(userPreferences);
	}
	
	private String setMessageboardNick (JdbcTemplate jdbc, UserPreferencesProviderService userPreferencesProvider, String uid, String nick) {
		/*if(messageboardNickExists(jdbc, nick)) {
			return "NICK_EXISTS";
		}
		
		jdbc.update("UPDATE messageboard_user_preferences SET messageboard_nick = ? WHERE userid = ? LIMIT 1", new Object[] { nick, uid } );*/
		userPreferencesProvider.setProperty("nick", nick, uid, "messageboard");
		return "OK";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String getMessages (JdbcTemplate jdbc, UserPreferencesProviderService userPreferencesProvider, String uid, int from, boolean decorateLinks, String url) {
		
		int count = 5;
		
		String preferenceCount = userPreferencesProvider.getProperty("count", uid, "messageboard");
		
		if (preferenceCount != null && preferenceCount != "") {
			count = Integer.parseInt(preferenceCount);
		}
		
		List<LinkedHashMap> messages = jdbc.findAll(
			"SELECT pr.preference_value AS messageboard_nick, datetime, text " +
			"  FROM messageboard_messages m " +
			"  LEFT JOIN user_preferences pr ON m.userid = pr.user " +
			" WHERE url = ? AND pr.preference_name LIKE 'messageboard_nick' " +
			"ORDER BY m.id DESC " +
			"LIMIT ?, ?", 
			new Object[] {url, from, count }, 
			new ResultProcessor<LinkedHashMap>() {
				@Override
				public LinkedHashMap processRow(ResultSet rs) throws SQLException {
					LinkedHashMap m = new LinkedHashMap();
					m.put("text", decorateLinks(rs.getString("text")));
					m.put("nick", rs.getString("messageboard_nick"));
					m.put("time", formatDatetime(rs.getString("datetime")));
					return m;
				}
			}
		);
		
		long messageCount = jdbc.queryFor("SELECT COUNT(*) FROM messageboard_messages WHERE url = ?", new Object[] { url }, Long.class);

		LinkedHashMap messageList = new LinkedHashMap();
		messageList.put("messages", messages);
		messageList.put("total", messageCount);
		
		return JSON.defaultJSON().forValue(messageList);
	}
	
	private Boolean messageboardNickExists (JdbcTemplate jdbc, String nick) {
		long nickExists = jdbc.queryFor("SELECT COUNT(*) FROM messageboard_user_preferences WHERE messageboard_nick = ? LIMIT 1", 
				new Object[] { nick }, Long.class);
		
		return nickExists > 0;
	}
	
	private String addMessage (JdbcTemplate jdbc, String uid, String nick, String messageText, String url) {
		jdbc.insert("INSERT INTO messageboard_messages (userid, datetime, url, text) VALUES (?, NOW(), ?, ?)", 
				new Object[] { uid, url, messageText });
		
		return "OK";
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
		this.defaultLanguage = props.getProperty("defaultLanguage", "sk");
		this.defaultVisibility = props.getProperty("defaultVisibility", "0");
		return super.start(props);
	}

	@Override
	public void desiredRequestServices (
			Set<Class<? extends ProxyService>> desiredServices,
			RequestHeader clientRQHeader) {
		super.desiredRequestServices(desiredServices, clientRQHeader);
		desiredServices.add(ModifiableStringService.class); //FIXME: toto je docasny hack kvoli late processingu, spravne tu ma byt len StringContentService
		desiredServices.add(DatabaseConnectionProviderService.class);
	}
}