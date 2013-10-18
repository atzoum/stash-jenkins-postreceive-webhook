package com.nerdwin15.stash.webhook.service.eligibility;

import java.util.List;

import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.user.StashUser;

/**
 * A concrete implementation of the EligiblityFilterChain.
 * 
 * @author Michael Irwin (mikesir87)
 */
public class ConcreteEligibilityFilterChain implements EligibilityFilterChain {
  
  private List<EligibilityFilter> filters;
  
  /**
   * Construct a new instance with the provided filters
   * @param filters The EligibilityFilters to be used.
   */
  public ConcreteEligibilityFilterChain(List<EligibilityFilter> filters) {
    this.filters = filters;
  }
  
  @Override
  public boolean shouldDeliverNotification(StashUser user, Repository repository) {
    for (EligibilityFilter filter : filters) {
      if (!filter.shouldDeliverNotification(user, repository))
        return false;
    }
    return true;
  }

}
