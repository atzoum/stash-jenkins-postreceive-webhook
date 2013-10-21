package com.nerdwin15.stash.webhook.service.eligibility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.stash.event.RepositoryRefsChangedEvent;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.StashUser;

/**
 * Test case for the {@link ConcreteEligibilityFilterChain} class.
 * 
 * @author Michael Irwin (mikesir87)
 */
public class ConcreteEligibilityFilterChainTest {

  private ConcreteEligibilityFilterChain filterChain;
  private List<EligibilityFilter> filters = new ArrayList<EligibilityFilter>();
  private EligibilityFilter filter;
  private StashUser user;
  private Repository repo;
  
  /**
   * Setup tasks
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    filter = mock(EligibilityFilter.class);
    filters.add(filter);
    filterChain = new ConcreteEligibilityFilterChain(filters);
    user = mock(StashUser.class);
    repo = mock(Repository.class);
  }
  
  /**
   * Validate that the chain should deliver if the internal filter says so.
   * @throws Exception
   */
  @Test
  public void shouldDeliverIfFilterSaysSo() throws Exception {
    when(filter.shouldDeliverNotification(user,repo)).thenReturn(true);
    assertTrue(filterChain.shouldDeliverNotification(user,repo));
  }
  
  /**
   * Validate that the chain should cancel delivery if the internal filter says
   * so.
   * @throws Exception
   */
  @Test
  public void shouldNotDeliverIfFilterSaysSo() throws Exception {
    when(filter.shouldDeliverNotification(user,repo)).thenReturn(false);
    assertFalse(filterChain.shouldDeliverNotification(user,repo));
  }
}
