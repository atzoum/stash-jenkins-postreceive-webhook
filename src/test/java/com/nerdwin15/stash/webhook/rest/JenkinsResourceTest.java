package com.nerdwin15.stash.webhook.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.atlassian.stash.i18n.I18nService;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.nav.NavBuilder.Repo;
import com.atlassian.stash.nav.NavBuilder.RepoClone;
import com.atlassian.stash.project.Project;
import com.atlassian.stash.repository.Repository;
import com.atlassian.stash.ssh.api.SshCloneUrlResolver;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;
import com.nerdwin15.stash.webhook.Notifier;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Test case for the JenkinsResource class.
 * 
 * @author Michael Irwin (mikesir87)
 */
public class JenkinsResourceTest {

  private static final String JENKINS_BASE = "http://jenkins.localhost/jenkins";
  private static final String CLONE_URL = "ssh://git@stash:7999/test/test.git";
  private static final boolean IGNORE_CERTS = false;

  private static final String HTTP_URL = 
      "https://stash.localhost/stash/scm/test/test.git";
  private static final String SSH_URL = 
      "ssh://git@stash.localhost:7999/test/test.git";
  
  private JenkinsResource resource;
  private Notifier notifier; 
  private PermissionValidationService permissionValidationService; 
  private I18nService i18nService;
  private NavBuilder navBuilder;
  private SshCloneUrlResolver sshCloneUrlResolver;
  
  private Repository repository;
  
  /**
   * Setup tasks
   */
  @Before
  public void setup() throws Exception {
    notifier = mock(Notifier.class);
    permissionValidationService = mock(PermissionValidationService.class);
    i18nService = mock(I18nService.class);
    navBuilder = mock(NavBuilder.class);
    sshCloneUrlResolver = mock(SshCloneUrlResolver.class);
    
    resource = new JenkinsResource(notifier, permissionValidationService, 
        i18nService, navBuilder, sshCloneUrlResolver);
    
    repository = mock(Repository.class);
    Project project = mock(Project.class);
    when(repository.getProject()).thenReturn(project);
    when(project.getKey()).thenReturn("KEY");
    when(repository.getSlug()).thenReturn("SLUG");
  }

  /**
   * Validate that if a null JenkinsBase is provided, a BAD_REQUEST is returned.
   */
  @Test
  public void shouldFailWhenJenkinsBaseNullProvidedToTest() {
    Response response = resource.test(repository, null, CLONE_URL, IGNORE_CERTS);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  /**
   * Validate that if a null repo clone url is provided, a BAD_REQUEST is 
   * returned.
   */
  @Test
  public void shouldFailWhenCloneUrlNullProvidedToTest() {
    Response response = resource.test(repository, JENKINS_BASE, null, IGNORE_CERTS);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }
  
  /**
   * Validate that the test should fail if the notifier returns null.
   */
  @Test
  public void shouldFailTestWhenNotifierReturnsNull() {
    Response response = fireTest(null);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    assertEquals("FAIL", response.getEntity());
  }
  
  /**
   * Validate that the test should fail if the notifier returns something 
   * unexpected.
   */
  @Test
  public void shouldFailTestWhenNotifierReturnsUnexpectedResult() {
    Response response = fireTest("Something unexpected");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    assertEquals("FAIL", response.getEntity());
  }
  
  /**
   * Validate that the test should pass if the notifier returns the expected
   * value.
   */
  @Test
  public void shouldPassTestWhenNotifierReturnsExpectedResult() {
    Response response = fireTest("Scheduled some sort of poll");
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    assertNull(response.getEntity());
  }
  
  /**
   * Validate that the config endpoint creates the expected results.
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testConfigResource() {
    Repo repo = mock(Repo.class);
    RepoClone repoClone = mock(RepoClone.class);
    
    when(navBuilder.repo(repository)).thenReturn(repo);
    when(repo.clone("git")).thenReturn(repoClone);
    when(repoClone.buildAbsoluteWithoutUsername()).thenReturn(HTTP_URL);
    
    when(sshCloneUrlResolver.getCloneUrl(repository)).thenReturn(SSH_URL);
    
    Response response = resource.config(repository);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    Map<String, String> data = (Map<String, String>) response.getEntity();
    assertEquals(data.get("ssh"), SSH_URL);
    assertEquals(data.get("http"), HTTP_URL);
    
    verify(sshCloneUrlResolver).getCloneUrl(repository);
    verify(repoClone).buildAbsoluteWithoutUsername();
  }
  
  private Response fireTest(String jenkinsResponse) {
    when(notifier.notify(repository, JENKINS_BASE, IGNORE_CERTS, CLONE_URL))
      .thenReturn(jenkinsResponse);
    
    Response response = resource.test(repository, JENKINS_BASE, CLONE_URL, 
        IGNORE_CERTS);
    
    verify(permissionValidationService)
      .validateForRepository(repository, Permission.REPO_ADMIN);
    verify(notifier).notify(repository, JENKINS_BASE, IGNORE_CERTS, CLONE_URL);
    
    return response;
  }
  
}
