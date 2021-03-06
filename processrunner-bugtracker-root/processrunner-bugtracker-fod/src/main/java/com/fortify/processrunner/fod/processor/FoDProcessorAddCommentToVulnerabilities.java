package com.fortify.processrunner.fod.processor;

import java.net.URI;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.fortify.processrunner.context.Context;
import com.fortify.processrunner.fod.context.IContextFoD;
import com.fortify.processrunner.processor.AbstractProcessor;
import com.fortify.processrunner.processor.IProcessor;
import com.fortify.util.rest.IRestConnection;
import com.fortify.util.spring.SpringExpressionUtil;
import com.fortify.util.spring.expression.SimpleExpression;
import com.fortify.util.spring.expression.TemplateExpression;
import com.sun.jersey.api.client.WebResource;

/**
 * <p>This {@link IProcessor} implementation will add a comment to one or more FoD
 * vulnerabilities indicating that the vulnerability has been submitted to a
 * bug tracker. The vulnerability id's for which to add a comment are determined
 * by evaluating configured expressions.</p>
 * 
 * <p>Configured expressions are evaluated as follows:</p>
 * <ul>
 * <li>{@link #setRootExpression(SimpleExpression)} defines the root object on which other 
 *     expressions will be evaluated. If not defined, the current {@link Context} instance 
 *     will be used as the root object.</li>
 * <li>{@link #setIterableExpression(SimpleExpression)} defines the expression to retrieve 
 *     an {@link Iterable} instance from the root object. If not defined, the current root 
 *     object will be used for determining the vulnerability id for which to add a comment.</li>
 * <li>{@link #setVulnIdExpression(SimpleExpression)} defines the expression to retrieve
 *     the actual vulnerability id from either the root object, or from each object returned
 *     by the {@link Iterable} instance. The default expression is 'vulnId'.</li>   
 * </ul>
 * 
 * <p>Usually this class is not used directly, but either 
 * {@link FoDProcessorAddCommentToVulnerabilitiesNonGrouped} or
 * {@link FoDProcessorAddCommentToVulnerabilitiesGrouped} is used.</p>
 */
public class FoDProcessorAddCommentToVulnerabilities extends AbstractProcessor {
	private static final Log LOG = LogFactory.getLog(AbstractProcessor.class);
	private TemplateExpression uriTemplateExpression = SpringExpressionUtil.parseTemplateExpression("/api/v3/releases/${[FoDReleaseId]}/vulnerabilities/bulk-edit");
	private SimpleExpression rootExpression;
	private SimpleExpression iterableExpression;
	private SimpleExpression vulnIdExpression = SpringExpressionUtil.parseSimpleExpression("vulnId");
	private TemplateExpression commentTemplateExpression = SpringExpressionUtil.parseTemplateExpression("--- Vulnerability submitted to ${SubmittedIssueBugTrackerName}: ${SubmittedIssueId==null?'':'ID '+SubmittedIssueId} Location ${SubmittedIssueLocation}");
	
	@Override
	protected boolean process(Context context) {
		JSONArray vulnIds = getVulnIds(context);
		String comment = SpringExpressionUtil.evaluateExpression(context, getCommentTemplateExpression(), String.class);
		bulkEditComments(context, vulnIds, comment);
		return true;
	}

	private void bulkEditComments(Context context, JSONArray vulnIds, String comment) {
		if ( LOG.isDebugEnabled() ) {
			try {
				LOG.debug("Adding comment '"+comment+"' for vulnerability id's "+vulnIds.join(", "));
			} catch (JSONException e) {
				LOG.error("Error joining JSONArray", e);
			}
		}

		IRestConnection conn = context.as(IContextFoD.class).getFoDConnectionRetriever().getConnection();
		WebResource resource = conn.getBaseResource().uri(
				SpringExpressionUtil.evaluateExpression(context, getUriTemplateExpression(), URI.class));
		LOG.debug("Add bulk comments using "+resource);
		JSONObject data = getBulkEditCommentsJSONObject(vulnIds, comment);
		conn.executeRequest(HttpMethod.POST, resource.entity(data,MediaType.APPLICATION_JSON), JSONObject.class);
	}

	private JSONObject getBulkEditCommentsJSONObject(JSONArray vulnIds, String comment) {
		try {
			JSONObject root = new JSONObject();
			JSONObject data = new JSONObject();
			root.put("requestModel", data);
			data.put("comment", comment);
			data.put("vulnerabilityIds", vulnIds);
			return data;
		} catch ( JSONException e ) {
			throw new RuntimeException("Cannot create FoD bulk edit request", e);
		}
	}

	private JSONArray getVulnIds(Context context) {
		JSONArray vulnIds = new JSONArray();
		Object root = rootExpression==null?context:SpringExpressionUtil.evaluateExpression(context, getRootExpression(), Object.class);
		if ( getIterableExpression() == null ) {
			vulnIds.put(SpringExpressionUtil.evaluateExpression(root, getVulnIdExpression(), String.class));
		} else {
			Iterable<?> iterable = SpringExpressionUtil.evaluateExpression(root, getIterableExpression(), Iterable.class);
			for ( Object o : iterable ) {
				vulnIds.put(SpringExpressionUtil.evaluateExpression(o, getVulnIdExpression(), String.class));
			}
		}
		return vulnIds;
	}

	public TemplateExpression getUriTemplateExpression() {
		return uriTemplateExpression;
	}

	public void setUriTemplateExpression(TemplateExpression uriTemplateExpression) {
		this.uriTemplateExpression = uriTemplateExpression;
	}
	
	

	public SimpleExpression getRootExpression() {
		return rootExpression;
	}

	public void setRootExpression(SimpleExpression rootExpression) {
		this.rootExpression = rootExpression;
	}

	public SimpleExpression getIterableExpression() {
		return iterableExpression;
	}

	public void setIterableExpression(SimpleExpression iterableExpression) {
		this.iterableExpression = iterableExpression;
	}

	public SimpleExpression getVulnIdExpression() {
		return vulnIdExpression;
	}

	public void setVulnIdExpression(SimpleExpression vulnIdExpression) {
		this.vulnIdExpression = vulnIdExpression;
	}

	public TemplateExpression getCommentTemplateExpression() {
		return commentTemplateExpression;
	}

	public void setCommentTemplate(TemplateExpression commentTemplateExpression) {
		this.commentTemplateExpression = commentTemplateExpression;
	}
}
