package com.nerdwin15.stash.webhook;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.hook.repository.RepositoryHook;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.nerdwin15.stash.webhook.service.HttpClientFactory;
import com.nerdwin15.stash.webhook.service.SettingsService;

/**
 * Service object that does the actual notification.
 * 
 * @author Michael Irwin (mikesir87)
 * @author Peter Leibiger (kuhnroyal)
 */
public class Notifier {

  /**
   * Key for the repository hook
   */
  public static final String KEY = 
      "com.nerdwin15.stash-stash-webhook-jenkins:jenkinsPostReceiveHook";
  
  /**
   * Field name for the Jenkins base URL property
   */
  public static final String JENKINS_BASE = "jenkinsBase";

  /**
   * Field name for the Repo Clone Url property
   */
  public static final String CLONE_URL = "gitRepoUrl";
  
  /**
   * Field name for the ignore certs property
   */
  public static final String IGNORE_CERTS = "ignoreCerts";

  /**
   * Field name for the ignore committers property
   */
  public static final String IGNORE_COMMITTERS = "ignoreCommitters";
  
  /**
   * Field name for the notify for pull requests
   */
  public static final String NOTIFY_PULL_REQUESTS = "notifyPullRequests";
  
  /**
   * Field name for the notify for pull requests notification Url
   */
  public static final String PULL_REQUESTS_NOTIFICATION_URL = "pullRequestsNotificationUrl";
  
  

  private static final Logger LOGGER = 
      LoggerFactory.getLogger(Notifier.class);
  private static final String URL = "%s/git/notifyCommit?url=%s";

  private final HttpClientFactory httpClientFactory;
  private final SettingsService settingsService;

  /**
   * Create a new instance
   * @param settingsService Service used to get webhook settings
   * @param httpClientFactory Factory to generate HttpClients
   */
  public Notifier(SettingsService settingsService,
      HttpClientFactory httpClientFactory) {
    
    this.httpClientFactory = httpClientFactory;
    this.settingsService = settingsService;
  }

  /**
   * Send notification to Jenkins for the provided repository.
   * @param repo The repository to base the notification on.
   * @return Text result from Jenkins
   */
  public @Nullable String notify(@Nonnull Repository repo) { //CHECKSTYLE:annot
    final RepositoryHook hook = settingsService.getRepositoryHook(repo);
    final Settings settings = settingsService.getSettings(repo);
    if (hook == null || !hook.isEnabled() || settings == null) {
      LOGGER.debug("Hook not configured correctly or not enabled, returning.");
      return null;
    }

    return notify(repo, settings.getString(JENKINS_BASE), 
        settings.getBoolean(IGNORE_CERTS, false),
        settings.getString(CLONE_URL));
  }

  /**
   * Send notification to Jenkins using the provided settings
   * @param repo The repository to base the notification on.
   * @param jenkinsBase Base URL for Jenkins instance
   * @param ignoreCerts True if all certs should be allowed
   * @param cloneUrl The repository url
   * @return The response body for the notification.
   */
  public @Nullable String notify(@Nonnull Repository repo, //CHECKSTYLE:annot
      String jenkinsBase, boolean ignoreCerts, String cloneUrl) {
    
    HttpClient client = null;
    final String url = getUrl(repo, maybeReplaceSlash(jenkinsBase),
        cloneUrl);

    try {
      client = httpClientFactory.getHttpClient(url.startsWith("https"), 
          ignoreCerts);

      HttpResponse response = client.execute(new HttpGet(url));
      LOGGER.debug("Successfully triggered jenkins with url '{}': ", url);
      InputStream content = response.getEntity().getContent();
      return CharStreams.toString(
          new InputStreamReader(content, Charsets.UTF_8));
    } catch (Exception e) {
      LOGGER.error("Error triggering jenkins with url '" + url + "'", e);
    } finally {
      if (client != null) {
        client.getConnectionManager().shutdown();
        LOGGER.debug("Successfully shutdown connection");
      }
    }
    return null;
  }
  
  public @Nullable String notifyPullRequest(@Nonnull PullRequestEvent event) {
	  PullRequest pullRequest = event.getPullRequest();
	  final Repository repo = pullRequest.getToRef().getRepository();
	  final RepositoryHook hook = settingsService.getRepositoryHook(repo);
	    final Settings settings = settingsService.getSettings(repo);
	    if (hook == null || !hook.isEnabled() || settings == null) {
	      LOGGER.debug("Hook not configured correctly or not enabled, returning.");
	      return null;
	    }
	    
	    final Boolean notify = settings.getBoolean(NOTIFY_PULL_REQUESTS, false);
	    if (!notify) {
	    	LOGGER.debug("Pull requests notifications not enabled, returning.");
	    	return null;
	    }
	    
	    String eventName = event.getClass().getSimpleName().replaceAll("PullRequest", "").replaceAll("Event", "").toLowerCase();
	    String url = settings.getString(PULL_REQUESTS_NOTIFICATION_URL);
	    final boolean ignoreCerts = settings.getBoolean(IGNORE_CERTS, false);
	    url = url.replaceAll("\\$pr", urlEncode(String.valueOf(pullRequest.getId())))
	    		.replaceAll("\\$fr", urlEncode(pullRequest.getFromRef().getId()))
	    		.replaceAll("\\$fh", urlEncode(pullRequest.getFromRef().getLatestChangeset()))
	    		.replaceAll("\\$tr", urlEncode(pullRequest.getToRef().getId()))
	    		.replaceAll("\\$th", urlEncode(pullRequest.getToRef().getLatestChangeset()))
	    		.replaceAll("\\$event", urlEncode(eventName))
	    		;
	    LOGGER.debug("About to send request to url: {}",url);
	    HttpClient client = null;
	        try {
	          client = httpClientFactory.getHttpClient(url.startsWith("https"), ignoreCerts);

	          HttpResponse response = client.execute(new HttpGet(url));
	          LOGGER.debug("Successfully triggered jenkins with url '{}': ", url);
	          InputStream content = response.getEntity().getContent();
	          return CharStreams.toString(
	              new InputStreamReader(content, Charsets.UTF_8));
	        } catch (Exception e) {
	          LOGGER.error("Error triggering jenkins with url '" + url + "'", e);
	        } finally {
	          if (client != null) {
	            client.getConnectionManager().shutdown();
	            LOGGER.debug("Successfully shutdown connection");
	          }
	        }
	        return null;
  }

  /**
   * Get the url for notifying of Jenkins. Protected for testing purposes
   * @param repository The repository to base the request to.
   * @param jenkinsBase The base URL of the Jenkins instance
   * @param cloneUrl The url used for cloning the repository
   * @return The url to use for notifying Jenkins
   */
  protected String getUrl(Repository repository, String jenkinsBase, 
      String cloneUrl) {
    return String.format(URL, jenkinsBase, urlEncode(cloneUrl));
  }
  
  private static String urlEncode(String string) {
    try {
      return URLEncoder.encode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private String maybeReplaceSlash(String string) {
    return string == null ? null : string.replaceFirst("/$", "");
  }
}