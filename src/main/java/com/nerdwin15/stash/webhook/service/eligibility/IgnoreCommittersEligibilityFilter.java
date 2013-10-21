package com.nerdwin15.stash.webhook.service.eligibility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.StashUser;
import com.nerdwin15.stash.webhook.Notifier;
import com.nerdwin15.stash.webhook.service.SettingsService;

/**
 * An EligibilityFilter that checks if the user that initiated the 
 * RepositoryRefsChangedEvent is a user that is in the ignores list for the 
 * hook configuration.
 * 
 * @author Michael Irwin (mikesir87)
 */
public class IgnoreCommittersEligibilityFilter implements EligibilityFilter {

  private static final Logger logger = // CHECKSTYLE:logger
  LoggerFactory.getLogger(IgnoreCommittersEligibilityFilter.class);

  private SettingsService settingsService;

  /**
   * Constructs a new instance
   * @param settingsService Service to get the webhook settings
   */
  public IgnoreCommittersEligibilityFilter(
      SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @Override
  public boolean shouldDeliverNotification(StashUser user, Repository repository) {
    String eventUserName = user.getName();

    final Settings settings = settingsService.getSettings(repository);
    String ignoreCommitters = settings.getString(Notifier.IGNORE_COMMITTERS);
    if (ignoreCommitters == null)
      return true;

    for (String committer : ignoreCommitters.split(" ")) {
      if (committer.equalsIgnoreCase(eventUserName)) {
        logger.debug("Ignoring push event due to ignore committer {}",
            committer);
        return false;
      }
    }
    return true;
  }
}
