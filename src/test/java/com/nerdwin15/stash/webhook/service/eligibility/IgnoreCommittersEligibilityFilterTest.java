package com.nerdwin15.stash.webhook.service.eligibility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.StashUser;
import com.nerdwin15.stash.webhook.Notifier;
import com.nerdwin15.stash.webhook.service.SettingsService;

/**
 * Test case for the {@link IgnoreCommittersEligibilityFilter} class
 * 
 * @author Michael Irwin (mikesir87)
 */
public class IgnoreCommittersEligibilityFilterTest {

  private SettingsService settingsService;
  private IgnoreCommittersEligibilityFilter filter;
  private Settings settings;
  private Repository repo;
  private StashUser user;
  private EventContext eventContext;
  private String username = "pinky";
  
  /**
   * Setup tasks
   */
  @Before
  public void setUp() throws Exception {
    settingsService = mock(SettingsService.class);
    repo = mock(Repository.class);
    user = mock(StashUser.class);
    when(user.getName()).thenReturn(username);
    filter = new IgnoreCommittersEligibilityFilter(settingsService);
    settings = mock(Settings.class);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    
    eventContext = mock(EventContext.class);
    when(eventContext.getEventSource()).thenReturn(null);
    when(eventContext.getRepository()).thenReturn(repo);
    when(eventContext.getUser()).thenReturn(user);
  }
  
  /**
   * Validate that the filter should still allow delivery when no ignored
   * committers settings have been set.
   * @throws Exception
   */
  @Test
  public void shouldAllowWhenIgnoredCommittersNull() throws Exception {
    when(settings.getString(Notifier.IGNORE_COMMITTERS)).thenReturn(null);
    assertTrue(filter.shouldDeliverNotification(eventContext));
  }
  
  /**
   * Validate that the filter should still allow delivery when the event user
   * does not match any of the ignored committers
   * @throws Exception
   */
  @Test
  public void shouldAllowWhenIgnoredCommittersDoesntMatch() throws Exception {
    when(settings.getString(Notifier.IGNORE_COMMITTERS))
      .thenReturn(username + "-notmatching");
    assertTrue(filter.shouldDeliverNotification(eventContext));
  }
  
  /**
   * Validate that the filter should cancel if an ignored committer matches
   * @throws Exception
   */
  @Test
  public void shouldCancelWhenIgnoredCommittersMatches() throws Exception {
    when(settings.getString(Notifier.IGNORE_COMMITTERS)).thenReturn(username);
    assertFalse(filter.shouldDeliverNotification(eventContext));
  }
  
  /**
   * Validate that the filter should cancel if an ignored committer matches
   * @throws Exception
   */
  @Test
  public void shouldCancelWhenMatchesWithMultipleCommitters() throws Exception {
    when(settings.getString(Notifier.IGNORE_COMMITTERS)).thenReturn(username 
        + " anotherUser");
    assertFalse(filter.shouldDeliverNotification(eventContext));
  }
  
}
