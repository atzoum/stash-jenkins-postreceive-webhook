package com.nerdwin15.stash.webhook;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.atlassian.stash.event.RepositoryRefsChangedEvent;
import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.setting.Settings;
import com.atlassian.stash.user.StashUser;
import com.nerdwin15.stash.webhook.service.SettingsService;
import com.nerdwin15.stash.webhook.service.eligibility.EligibilityFilterChain;
import com.nerdwin15.stash.webhook.service.eligibility.EventContext;

/**
 * Test case for the RepositoryChangeListener class.
 * 
 * @author Michael Irwin (mikesir87)
 */
public class RepositoryChangeListenerTest {

  private Notifier notifier;
  private EligibilityFilterChain filterChain;
  private RepositoryChangeListener listener;
  private SettingsService settingsService;

  /**
   * Setup tasks
   */
  @Before
  public void setup() throws Exception {
    notifier = mock(Notifier.class);
    filterChain = mock(EligibilityFilterChain.class);
    settingsService = mock(SettingsService.class);
    listener = new RepositoryChangeListener(filterChain, notifier, 
        settingsService);
  }

  /**
   * Validates that the notifier is used when the filter chain says ok
   */
  @Test
  public void shouldNotifyWhenChainSaysOk() throws Exception {
    RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);
    StashUser u = mock(StashUser.class);
    Repository repo = mock(Repository.class);
    Settings settings = mock(Settings.class);

    LinkedList<RefChange> lst = new LinkedList<RefChange>();

    when(e.getRefChanges()).thenReturn(lst);

    RefChange r = mock(RefChange.class);
    when(r.getToHash()).thenReturn("sha1");
    when(r.getRefId()).thenReturn("refs/heads/master");
    lst.add(r);

    StashUser user = mock(StashUser.class);
    String username = "pinky";
    ArgumentCaptor<EventContext> contextCaptor = 
        ArgumentCaptor.forClass(EventContext.class);
    when(e.getUser()).thenReturn(user);
    when(user.getName()).thenReturn(username);
    
    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    when(filterChain.shouldDeliverNotification(contextCaptor.capture()))
        .thenReturn(true);

    listener.onRefsChangedEvent(e);

    verify(notifier).notifyBackground(repo, "master", "sha1");
    assertEquals(e, contextCaptor.getValue().getEventSource());
    assertEquals(username, contextCaptor.getValue().getUser().getName());
    assertEquals(repo, contextCaptor.getValue().getRepository());
  }

  /**
   * Validates that everything works fine when the event has a null user
   */
  @Test
  public void shouldWorkFineWithNullUser() throws Exception {
    RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);

    LinkedList<RefChange> lst = new LinkedList<RefChange>();

    when(e.getRefChanges()).thenReturn(lst);

    RefChange r = mock(RefChange.class);
    when(r.getToHash()).thenReturn("sha1");
    when(r.getRefId()).thenReturn("refs/heads/master");
    lst.add(r);

    Repository repo = mock(Repository.class);
    Settings settings = mock(Settings.class);

    ArgumentCaptor<EventContext> contextCaptor = 
        ArgumentCaptor.forClass(EventContext.class);
    when(e.getUser()).thenReturn(null);
    
    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    when(filterChain.shouldDeliverNotification(contextCaptor.capture()))
        .thenReturn(true);

    listener.onRefsChangedEvent(e);

    verify(notifier).notifyBackground(repo, "master", "sha1");
    assertEquals(e, contextCaptor.getValue().getEventSource());
    assertEquals(null, contextCaptor.getValue().getUser());
    assertEquals(repo, contextCaptor.getValue().getRepository());
  }

  /**
   * Validates that the notifier is not notified when the filter chain says no
   */
  @Test
  public void shouldNotifyWhenChainSaysCancel() throws Exception {
	RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);
	StashUser u = mock(StashUser.class);
	Repository repo = mock(Repository.class);
	Settings settings = mock(Settings.class);

    LinkedList<RefChange> lst = new LinkedList<RefChange>();
    when(e.getRefChanges()).thenReturn(lst);

    RefChange r = mock(RefChange.class);
    when(r.getToHash()).thenReturn("sha1");
    when(r.getRefId()).thenReturn("refs/heads/master");
    lst.add(r);

    StashUser user = mock(StashUser.class);
    String username = "pinky";
    ArgumentCaptor<EventContext> contextCaptor = 
        ArgumentCaptor.forClass(EventContext.class);
    when(e.getUser()).thenReturn(user);
    when(user.getName()).thenReturn(username);

    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    when(filterChain.shouldDeliverNotification(contextCaptor.capture()))
        .thenReturn(false);

    listener.onRefsChangedEvent(e);

    verify(notifier, never()).notifyBackground(repo, "master", "sha1");
    assertEquals(e, contextCaptor.getValue().getEventSource());
    assertEquals(username, contextCaptor.getValue().getUser().getName());
    assertEquals(repo, contextCaptor.getValue().getRepository());
  }
  
  /**
   * Validates that if the repository has no settings set, execution stops
   * @throws Exception
   */
  @Test
  public void shouldntConsultChainWhenSettingsAreNull() throws Exception {
    RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);
    Repository repo = mock(Repository.class);

    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(null);

    listener.onRefsChangedEvent(e);

    verify(notifier, never()).notifyBackground(repo, "master", "sha1");
  }
  
  /**
   * Validates that if there are no <code>RefChange</code>s in an event
   * there is no exception thrown
   */
  @Test
  public void emptyRefChangeDoesntCallNotifier() {
    RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);

    LinkedList<RefChange> lst = new LinkedList<RefChange>();
    when(e.getRefChanges()).thenReturn(lst);

    Repository repo = mock(Repository.class);
    Settings settings = mock(Settings.class);

    ArgumentCaptor<EventContext> contextCaptor =
        ArgumentCaptor.forClass(EventContext.class);
    when(e.getUser()).thenReturn(null);

    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    when(filterChain.shouldDeliverNotification(contextCaptor.capture()))
        .thenReturn(true);

    listener.onRefsChangedEvent(e);
    
    verifyNoMoreInteractions(notifier);
  }

  /**
   * Validates that the notifier is used when the filter chain says ok
   */
  @Test
  public void shouldNotifyAllRefsWhenMultiple() {
    RepositoryRefsChangedEvent e = mock(RepositoryRefsChangedEvent.class);
    Repository repo = mock(Repository.class);
    Settings settings = mock(Settings.class);

    LinkedList<RefChange> lst = new LinkedList<RefChange>();

    when(e.getRefChanges()).thenReturn(lst);

    RefChange r1 = mock(RefChange.class);
    when(r1.getToHash()).thenReturn("sha1");
    when(r1.getRefId()).thenReturn("refs/heads/master");
    lst.add(r1);

    RefChange r2 = mock(RefChange.class);
    when(r2.getToHash()).thenReturn("sha2");
    when(r2.getRefId()).thenReturn("refs/heads/feature/branch");
    lst.add(r2);

    StashUser user = mock(StashUser.class);
    String username = "pinky";
    ArgumentCaptor<EventContext> contextCaptor =
        ArgumentCaptor.forClass(EventContext.class);
    when(e.getUser()).thenReturn(user);
    when(user.getName()).thenReturn(username);

    when(e.getRepository()).thenReturn(repo);
    when(settingsService.getSettings(repo)).thenReturn(settings);
    when(filterChain.shouldDeliverNotification(contextCaptor.capture()))
        .thenReturn(true);

    listener.onRefsChangedEvent(e);

    List<EventContext> captures = contextCaptor.getAllValues();

    EventContext ctx = captures.get(0);
    verify(notifier).notifyBackground(repo, "master", "sha1");
    assertEquals(e, ctx.getEventSource());
    assertEquals(username, ctx.getUser().getName());
    assertEquals(repo, ctx.getRepository());

    ctx = captures.get(1);
    verify(notifier).notifyBackground(repo, "feature/branch", "sha2");
    assertEquals(e, ctx.getEventSource());
    assertEquals(username, ctx.getUser().getName());
    assertEquals(repo, ctx.getRepository());
  }
}
