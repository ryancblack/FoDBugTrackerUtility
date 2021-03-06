package com.fortify.processrunner.fod.connection;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import com.fortify.fod.connection.FoDConnectionRetrieverUserCredentials;
import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.context.ContextProperty;
import com.fortify.processrunner.context.IContextAware;
import com.fortify.processrunner.context.IContextPropertyProvider;
import com.fortify.util.rest.ProxyConfiguration;

public class ContextAwareFoDConnectionRetrieverUserCredentials 
	extends FoDConnectionRetrieverUserCredentials 
	implements IContextAware, IContextPropertyProvider 
{
	public void setContext(Context context) {
		updateConnectionProperties(context);
		ProxyConfiguration proxy = getProxy();
		if ( proxy!=null && proxy instanceof IContextAware ) {
			((IContextAware)proxy).setContext(context);
		}
	}
	
	public void addContextProperties(Collection<ContextProperty> contextProperties, Context context) {
		contextProperties.add(new ContextProperty(IContextFoDUserCredentials.PRP_BASE_URL, "FoD base URL", context,  StringUtils.isNotBlank(getBaseUrl())?getBaseUrl():"Read from console", false));
		contextProperties.add(new ContextProperty(IContextFoDUserCredentials.PRP_TENANT, "FoD tenant", context, StringUtils.isNotBlank(getTenant())?getTenant():"Read from console", false));
		contextProperties.add(new ContextProperty(IContextFoDUserCredentials.PRP_USER_NAME, "FoD user name", context, StringUtils.isNotBlank(getUserName())?getUserName():"Read from console", false));
		contextProperties.add(new ContextProperty(IContextFoDUserCredentials.PRP_PASSWORD, "FoD password", context, StringUtils.isNotBlank(getPassword())?"******":"Read from console", false));
		ProxyConfiguration proxy = getProxy();
		if ( proxy!=null && proxy instanceof IContextPropertyProvider ) {
			((IContextPropertyProvider)proxy).addContextProperties(contextProperties, context);
		}
	}
	
	protected void updateConnectionProperties(Context context) {
		IContextFoDUserCredentials ctx = context.as(IContextFoDUserCredentials.class);
		String baseUrl = ctx.getFoDBaseUrl();
		String tenant = ctx.getFoDTenant();
		String userName = ctx.getFoDUserName();
		String password = ctx.getFoDPassword();
		
		if ( !StringUtils.isBlank(baseUrl) ) {
			setBaseUrl(baseUrl);
		}
		if ( !StringUtils.isBlank(tenant) ) {
			setTenant(tenant);
		}
		if ( !StringUtils.isBlank(userName) ) {
			setUserName(userName);
		}
		if ( !StringUtils.isBlank(password) ) {
			setPassword(password);
		}		
		if ( getBaseUrl() == null) {
			setBaseUrl (System.console().readLine("FoD URL: "));
		}		
		if ( getTenant()==null ) {
			setTenant(System.console().readLine("FoD Tenant: "));
		}
		if ( getUserName()==null ) {
			setUserName(System.console().readLine("FoD User Name: "));
		}
		if ( getPassword()==null ) {
			setPassword(new String(System.console().readPassword("FoD Password: ")));
		}
	}
}
