package com.fortify.processrunner.jira.connection;

import com.fortify.util.rest.AbstractRestConnectionRetriever;
import com.fortify.util.rest.AuthenticatingRestConnection;
import com.fortify.util.rest.IRestConnection;
import com.fortify.util.rest.IRestConnectionRetriever;

/**
 * This {@link IRestConnectionRetriever} implementation is used to 
 * retrieve {@link IRestConnection} instances for JIRA based on
 * configured connection properties like JIRA base URL and credentials. 
 */
public class JiraConnectionRetriever extends AbstractRestConnectionRetriever implements IJiraConnectionRetriever {
	private String baseUrl;
	private String userName;
	private String password;
	
	public final IRestConnection createConnection() {
		return new AuthenticatingRestConnection(getBaseUrl(), getUserName(), getPassword());
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
