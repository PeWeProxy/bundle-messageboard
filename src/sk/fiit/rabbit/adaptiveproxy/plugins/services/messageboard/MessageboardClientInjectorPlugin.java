package sk.fiit.rabbit.adaptiveproxy.plugins.services.messageboard;

import sk.fiit.peweproxy.messages.ModifiableHttpResponse;
import sk.fiit.peweproxy.plugins.PluginProperties;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService;
import sk.fiit.rabbit.adaptiveproxy.plugins.servicedefinitions.HtmlInjectorService.HtmlPosition;
import sk.fiit.rabbit.adaptiveproxy.plugins.services.injector.JavaScriptInjectingProcessingPlugin;



public class MessageboardClientInjectorPlugin  extends JavaScriptInjectingProcessingPlugin {
	
	private String scriptUrl;
	String additionalHTML;
	
	@Override
	public ResponseProcessingActions processResponse(ModifiableHttpResponse response) {
		HtmlInjectorService htmlInjectionService = response.getServicesHandle().getService(HtmlInjectorService.class);
		String scripts = "<script src='" + scriptUrl + "'></script>";
		htmlInjectionService.inject(scripts + additionalHTML, HtmlPosition.ON_MARK);
		
		return ResponseProcessingActions.PROCEED;
	}
	
	
	@Override
	public boolean start(PluginProperties props) {
		scriptUrl = props.getProperty("scriptUrl");
		additionalHTML = props.getProperty("additionalHTML", "");
		
		return true;
	}
		
}
