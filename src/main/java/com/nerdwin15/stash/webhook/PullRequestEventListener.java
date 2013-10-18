package com.nerdwin15.stash.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventListener;
import com.atlassian.stash.event.pull.PullRequestDeclinedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.event.pull.PullRequestMergedEvent;
import com.atlassian.stash.event.pull.PullRequestOpenedEvent;
import com.atlassian.stash.event.pull.PullRequestRescopedEvent;
import com.atlassian.stash.setting.Settings;
import com.nerdwin15.stash.webhook.service.SettingsService;
import com.nerdwin15.stash.webhook.service.eligibility.EligibilityFilterChain;

public class PullRequestEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestEventListener.class);
	private final EligibilityFilterChain filterChain;
	private final Notifier notifier;
	private final SettingsService settingsService;
	
	public PullRequestEventListener(EligibilityFilterChain filterChain, Notifier notifier, SettingsService settingsService) {
		this.filterChain = filterChain;
	    this.notifier = notifier;
	    this.settingsService = settingsService;
	}
	
	@EventListener
	public void onPullRequestOpen(PullRequestOpenedEvent event) {
		LOGGER.debug("onPullRequestOpen: {}",event.getPullRequest().getId());
		onPullRequest(event);
	}
	
	@EventListener
	public void onPullRequestRescoped(PullRequestRescopedEvent event) {
		LOGGER.debug("onPullRequestRescoped: {}",event.getPullRequest().getId());
		onPullRequest(event);
	}
	
	@EventListener
	public void onPullRequestMerged(PullRequestMergedEvent event) {
		LOGGER.debug("onPullRequestMerged: {}",event.getPullRequest().getId());
		onPullRequest(event);
	}
	
	@EventListener
	public void onPullRequestDeclined(PullRequestDeclinedEvent event) {
		LOGGER.debug("onPullRequestDeclined: {}",event.getPullRequest().getId());
		onPullRequest(event);
	}
	
	
	
	public void onPullRequest(PullRequestEvent event) {
		Settings settings = settingsService.getSettings(event.getPullRequest().getToRef().getRepository());
	    if (settings == null) {
	    	return;
	    }
	    if (filterChain.shouldDeliverNotification(event.getUser(), event.getPullRequest().getToRef().getRepository()))
	        notifier.notifyPullRequest(event);
	}
}
