package sk.fiit.rabbit.adaptiveproxy.plugins.services.messageboard;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.simple.JSONObject;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

import sk.fiit.peweproxy.messages.HttpMessageFactory;
import sk.fiit.peweproxy.messages.HttpResponse;
import sk.fiit.peweproxy.messages.ModifiableHttpRequest;
import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.services.content.ModifiableStringService;
import sk.fiit.peweproxy.services.content.StringContentService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.DatabaseConnectionProviderService;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.common.SqlUtils;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;

public class KeyWordsProcessingPlugin  extends JavaScriptInjectingProcessingPlugin {
	
	
	@Override
	public HttpResponse getResponse(ModifiableHttpRequest request, HttpMessageFactory messageFactory) {
		StringContentService stringContentService = request.getServicesHandle().getService(StringContentService.class);
		
		Map<String, String> postData = getPostDataFromRequest(stringContentService.getContent());
		String content = "";
		Connection connection = null;
		try {
			connection = request.getServicesHandle().getService(DatabaseConnectionProviderService.class).getDatabaseConnection();

			if (request.getRequestHeader().getRequestURI().contains("action=getKeyWords")) {
				content = this.getKeyWords(connection, request.getRequestHeader().getField("Referer"), postData.get("checksum"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=editKeyWord")) {
				content = this.editKeyWord(connection, postData.get("id"), postData.get("term"), postData.get("relevance"), postData.get("type"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=removeKeyWord")) {
				content = this.removeKeyWord(connection, postData.get("id"));
			}
			if (request.getRequestHeader().getRequestURI().contains("action=addKeyWord")) {
				content = this.addKeyWord(connection, request.getRequestHeader().getField("Referer"), postData.get("checksum"), postData.get("term"), postData.get("relevance"), postData.get("type"));
			}
		} finally {
			SqlUtils.close(connection);
		}
		
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	private String getKeyWords(Connection connection, String url, String checksum) {
		
		PreparedStatement stmt = null;
		int dbTermId = -1;
		int pageId = -1;
		JSONObject keywords = new JSONObject();
		LinkedList l1 = new LinkedList();
		LinkedHashMap m1;
		
		try {
			stmt = connection.prepareStatement("SELECT * FROM `pages` WHERE `url` = ? AND `checksum` = ?;");
			stmt.setString(1, url);
			stmt.setString(2, checksum);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				pageId = rs.getInt(1);
			}
			
			stmt = connection.prepareStatement("SELECT `pages_terms`.`id`, `terms`.`label`, `pages_terms`.`weight`, `terms`.`term_type`, `pages_terms`.`source` FROM `pages_terms` JOIN `terms` ON `pages_terms`.`term_id` = `terms`.`id` WHERE `pages_terms`.`page_id` = ? AND `pages_terms`.`active` = 1;");
			stmt.setInt(1, pageId);
			stmt.execute();
			rs = stmt.getResultSet();
			while (rs.next()) {
				m1 = new LinkedHashMap();
				m1.put("id", rs.getString(1));
				m1.put("term", rs.getString(2));
				m1.put("relevance", rs.getString(3));
				m1.put("type", rs.getString(4));
				m1.put("source", rs.getString(5));
				l1.add(m1);
			}
			
		} catch (SQLException e) {
			logger.error("Couldget key words ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(stmt);
		}
		
		keywords.put("keywords", l1);
		
		String jsonString = keywords.toJSONString();
		return jsonString;
	}
	
	private String editKeyWord(Connection connection, String termId, String term, String relevance, String type) {
		PreparedStatement stmt = null;
		int dbTermId = 0;
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0, timestamp.indexOf("."));
		
		if (Float.parseFloat(relevance) > 1) return "FAIL";
		
		try {
			stmt = connection.prepareStatement("UPDATE `pages_terms` SET `weight` = ?, `source` = 'human', `updated_at` = ? WHERE `id` = ?;");
			stmt.setString(1, relevance);
			stmt.setString(2, formatedTimeStamp);
			stmt.setString(3, termId);
			stmt.execute();
			
			stmt = connection.prepareStatement("SELECT * FROM `pages_terms` WHERE `id` = ?;");
			stmt.setString(1, termId);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				dbTermId = rs.getInt(3);
			}
			
			stmt = connection.prepareStatement("UPDATE `terms` SET `label` = ?, `term_type` = ? WHERE `id` = ?;");
			stmt.setString(1, term);
			stmt.setString(2, type);
			stmt.setInt(3, dbTermId);
			stmt.execute();			
			
			return "OK";
		} catch (SQLException e) {
			logger.error("Could not edit key word ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(stmt);
		}
	}
	
	private String removeKeyWord(Connection connection, String termId) {
		
		PreparedStatement stmt = null;
		
		try {
			stmt = connection.prepareStatement("UPDATE `pages_terms` SET `active` = '0' WHERE `id` = ?;");
			stmt.setString(1, termId);
			stmt.execute();
			return "OK";
		} catch (SQLException e) {
			logger.error("Could not remove key word ", e);
			return "FAIL";
		} finally {
			SqlUtils.close(stmt);
		}
	}
	
	private String addKeyWord(Connection connection, String url, String checksum, String term, String relevance, String type) {
		PreparedStatement stmt = null;
		int pageId = -1;
		int resultCounter = 0;
		java.util.Date today = new java.util.Date();
		String timestamp = new Timestamp(today.getTime()).toString();
		String formatedTimeStamp = timestamp.substring(0, timestamp.indexOf("."));
		
		try {
			if (Float.parseFloat(relevance) > 1) return "FAIL";
		} catch (Exception e) {
			 return "FAIL";
		}
		
		try {
			stmt = connection.prepareStatement("SELECT * FROM `pages` WHERE `url` = ? AND `checksum` = ?;");
			stmt.setString(1, url);
			stmt.setString(2, checksum);
			stmt.execute();
			ResultSet rs = stmt.getResultSet();
			while (rs.next()) {
				pageId = rs.getInt(1);
			}
			
			if (pageId == -1) return "FAIL";
			
			stmt = connection.prepareStatement("SELECT `pages_terms`.`id`, `pages_terms`.`active` FROM `pages_terms` JOIN `terms` ON `pages_terms`.`term_id` = `terms`.`id` WHERE `pages_terms`.`page_id` = ? AND `terms`.`label` = ?;");
			stmt.setInt(1, pageId);
			stmt.setString(2, term);
			stmt.execute();
			rs = stmt.getResultSet();
			while (rs.next()) {
				resultCounter++;
				if (rs.getInt(2) == 0)
				{
					stmt = connection.prepareStatement("UPDATE `pages_terms` SET `active` = '1' WHERE `id` = ?;");
					rs.getInt(1);
					stmt.execute();
					break;
				}
				return "TERM_EXISTS";
			}
			
			stmt = connection.prepareStatement("INSERT INTO `terms` (`label`, `term_type`) VALUES (?, ?);");
			stmt.setString(1, term);
			stmt.setString(2, type);
			stmt.execute();
	
			
			stmt = connection.prepareStatement("INSERT INTO `pages_terms` (`page_id`, `term_id`, `weight`, `created_at`, `updated_at`, `source`) VALUES (?, LAST_INSERT_ID(), ?, ?, ?, ?);");
			stmt.setInt(1, pageId);
			stmt.setFloat(2, Float.parseFloat(relevance));
			stmt.setString(3, formatedTimeStamp);
			stmt.setString(4, formatedTimeStamp);
			stmt.setString(5, "human");
			stmt.execute();
			
			return "OK";
		} catch (SQLException e) {
			logger.error("Could not add key word ", e);
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
	
}
