package sk.fiit.rabbit.adaptiveproxy.plugins.services.messageboard;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

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

			if (request.getClientRequestHeader().getRequestURI().contains("action=setMessageboardNick")) {
				content = this.getKeyWords();
			}
			if (request.getClientRequestHeader().getRequestURI().contains("action=editKeyWord")) {
				content = this.editKeyWord();
			}
			if (request.getClientRequestHeader().getRequestURI().contains("action=removeKeyWord")) {
				content = this.removeKeyWord();
			}
			if (request.getClientRequestHeader().getRequestURI().contains("action=addKeyWord")) {
				content = this.addKeyWord();
			}
		} finally {
			SqlUtils.close(connection);
		}
		
		
		ModifiableHttpResponse httpResponse = messageFactory.constructHttpResponse(null, "text/html");
		ModifiableStringService stringService = httpResponse.getServicesHandle().getService(ModifiableStringService.class);
		stringService.setContent(content);
		
		return httpResponse;
	}
	
	// RETURN JSON
	private String getKeyWords() {
		return "";
	}
	
	// RETURN OK/FAIL/TERM_EXISTS
	private String editKeyWord() {
		return "";
	}
	
	// RETURN OK/FAIL
	private String removeKeyWord() {
		return "";
	}
	
	private String addKeyWord() {
		return "";
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
