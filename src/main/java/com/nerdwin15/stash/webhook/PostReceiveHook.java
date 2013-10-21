package com.nerdwin15.stash.webhook;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.atlassian.stash.hook.repository.AsyncPostReceiveRepositoryHook;
import com.atlassian.stash.hook.repository.RepositoryHookContext;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.RepositorySettingsValidator;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.setting.SettingsValidationErrors;
import com.google.common.base.Strings;

/**
 * Note that hooks can implement RepositorySettingsValidator directly.
 */
public class PostReceiveHook implements AsyncPostReceiveRepositoryHook, 
    RepositorySettingsValidator {
  
  @Override
  public void postReceive(@Nonnull RepositoryHookContext ctx, 
      @Nonnull Collection<RefChange> changes) {
    // Don't do anything since the event will be handled by the 
    // RepositoryChangeListener
  }

  @Override
  public void validate(@Nonnull Settings settings, 
      @Nonnull SettingsValidationErrors errors, 
      @Nonnull Repository repository) {
    
    final String jenkinsUrl = settings.getString(Notifier.JENKINS_BASE);
    if (Strings.isNullOrEmpty(jenkinsUrl)) {
      errors.addFieldError(Notifier.JENKINS_BASE, 
          "The url for your Jenkins instance is required.");
    }
    
    final String cloneType = settings.getString(Notifier.CLONE_URL);
    if (Strings.isNullOrEmpty(cloneType)) {
      errors.addFieldError(Notifier.CLONE_URL, 
          "The repository clone url is required");
    }
    
    final Boolean notifyOnPullRequests = settings.getBoolean(Notifier.NOTIFY_PULL_REQUESTS, false);
    if (notifyOnPullRequests) {
    	final String pullRequestNotificationUrl = settings.getString(Notifier.PULL_REQUESTS_NOTIFICATION_URL);
	    if (Strings.isNullOrEmpty(pullRequestNotificationUrl)) {
	      errors.addFieldError(Notifier.PULL_REQUESTS_NOTIFICATION_URL, 
	          "The pull request notification url is required");
	    }
    }
  }
}
