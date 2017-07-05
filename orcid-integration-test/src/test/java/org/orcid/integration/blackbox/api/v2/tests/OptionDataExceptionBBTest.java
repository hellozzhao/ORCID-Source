package org.orcid.integration.blackbox.api.v2.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orcid.integration.blackbox.api.v12.T2OAuthAPIService;
import org.orcid.integration.blackbox.api.v2.release.BlackBoxBaseV2Release;
import org.orcid.integration.blackbox.api.v2.release.MemberV2ApiClientImpl;
import org.orcid.jaxb.model.common_v2.Contributor;
import org.orcid.jaxb.model.common_v2.ContributorAttributes;
import org.orcid.jaxb.model.common_v2.ContributorOrcid;
import org.orcid.jaxb.model.common_v2.ContributorRole;
import org.orcid.jaxb.model.common_v2.CreditName;
import org.orcid.jaxb.model.common_v2.Title;
import org.orcid.jaxb.model.common_v2.Url;
import org.orcid.jaxb.model.common_v2.Visibility;
import org.orcid.jaxb.model.message.OrcidMessage;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.record_rc1.WorkExternalIdentifierType;
import org.orcid.jaxb.model.record_v2.ExternalID;
import org.orcid.jaxb.model.record_v2.Relationship;
import org.orcid.jaxb.model.record_v2.SequenceType;
import org.orcid.jaxb.model.record_v2.Work;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.jersey.api.client.ClientResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OptionDataExceptionBBTest extends BlackBoxBaseV2Release {
    
    @Resource(name = "memberV2ApiClient")
    private MemberV2ApiClientImpl memberV2ApiClient_release;
    
    @Resource(name = "t2OAuthClient_1_2")
    protected T2OAuthAPIService<ClientResponse> t2OAuthClient_1_2;
    
    private static int numWorks = -1;
    private static String accessToken = null;

    private List<URI> availableWorks = new ArrayList<URI>();
    
    @Test
    public void multiThreadedTest() {
        Runnable createWorksTask = () -> {
            while(true) {
                String random = RandomStringUtils.randomAlphanumeric(20);
                try {
                    URI uri = createNewWork(random);
                    availableWorks.add(uri);
                    System.out.println("created" + random);
                } catch (InterruptedException | JSONException e) {
                    fail();
                }
            }
        };
        
        Runnable updateWorksTask = () -> {
            while(availableWorks.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            while(true) {
                Double x = Math.floor(Math.random() * (availableWorks.size() - 1));
                URI toUpdate = availableWorks.get(x.intValue());
                try {
                    updateWork(toUpdate);
                    System.out.println("updated" + toUpdate.getPath());
                } catch (Exception e) {
                    fail();
                }
            }
        };
        
        Runnable viewWorksTask = () -> {
            while(availableWorks.isEmpty()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            while(true) {
                try {
                    getWorksOn12API();
                } catch (InterruptedException | JSONException e) {
                    fail();
                }
            }
        };
        
        Runnable deleteWorksTask = () -> {
            while(true) {
                try {
                    if(availableWorks.size() < 500) {
                        Thread.sleep(2500);
                        continue;
                    }
                    Double x = Math.floor(Math.random() * (availableWorks.size() - 1));
                    URI toDelete = availableWorks.get(x.intValue());
                    availableWorks.remove(toDelete);
                    String path = toDelete.getPath();
                    Long putCode = Long.valueOf(path.substring(path.lastIndexOf('/') + 1));                    
                    deleteWork(putCode);   
                    
                } catch (InterruptedException | JSONException e) {
                    fail();
                }
            }
        };
        
        System.out.println("Start");
        Thread t1 = new Thread(createWorksTask);
        t1.start();
        System.out.println("Create works");
        Thread t2 = new Thread(updateWorksTask);
        t2.start();
        System.out.println("Update works");
        Thread t3 = new Thread(viewWorksTask);
        t3.start();
        System.out.println("Delete works");
        Thread t4 = new Thread(deleteWorksTask);
        t4.start();
        
        System.out.println("Finish and waiting");
        while(true) {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    //Single thread test
    //@Test
    public void startTest() throws InterruptedException, JSONException {
        boolean shouldCreate = false;
        boolean shouldDelete = false;
        boolean shouldUpdate = false;
        
        Random r = new Random();
        
        // Create the first work
        try {
            String random = RandomStringUtils.randomAlphanumeric(20);
            URI uri = createNewWork(random);
            availableWorks.add(uri);
            System.out.println("created" + random);
        } catch (InterruptedException | JSONException e) {
            fail();
        }
        
        while(true) {
            shouldCreate = r.nextBoolean();
            shouldUpdate = r.nextBoolean();
            shouldDelete = r.nextBoolean() && r.nextBoolean(); //Lets make delete harder to happen
            System.out.println("Create: " + shouldCreate + " Update: " + shouldUpdate + " Delete: " + shouldDelete);
            if(shouldCreate) {
                String random = RandomStringUtils.randomAlphanumeric(20);
                try {
                    URI uri = createNewWork(random);
                    availableWorks.add(uri);
                    System.out.println("created" + random);
                } catch (InterruptedException | JSONException e) {
                    fail();
                }
            }
            
            if(shouldUpdate) {
                if(availableWorks.size() < 1) {
                    continue;
                }
                
                Double x = Math.floor(Math.random() * (availableWorks.size() - 1));
                URI toUpdate = availableWorks.get(x.intValue());
                try {
                    updateWork(toUpdate);
                    System.out.println("updated" + toUpdate.getPath());
                } catch (Exception e) {
                    fail();
                }
            }
            
            if(shouldDelete) {
                try {
                    if(availableWorks.size() < 500) {
                        continue;
                    }
                    Double x = Math.floor(Math.random() * (availableWorks.size() - 1));
                    URI toDelete = availableWorks.get(x.intValue());
                    availableWorks.remove(toDelete);
                    String path = toDelete.getPath();
                    Long putCode = Long.valueOf(path.substring(path.lastIndexOf('/') + 1));                    
                    deleteWork(putCode);   
                    
                } catch (InterruptedException | JSONException e) {
                    fail();
                }
            }
            
            getWorksOn12API();
        }                
    }
    
    private URI createNewWork(String workUniqueValue) throws InterruptedException, JSONException {
        String accessToken = getAccessToken();
        Work work1 = (Work) unmarshallFromPath("/record_2.0/samples/read_samples/work-2.0.xml", Work.class);
        work1.setPutCode(null);
        work1.setSource(null);
        work1.setVisibility(Visibility.PUBLIC);
        work1.getExternalIdentifiers().getExternalIdentifier().clear();
        org.orcid.jaxb.model.record_v2.WorkTitle title1 = new org.orcid.jaxb.model.record_v2.WorkTitle();
        title1.setTitle(new Title("Work # 1" + workUniqueValue));
        work1.setWorkTitle(title1);
        work1.getExternalIdentifiers().getExternalIdentifier().clear();
        for(int i = 0; i < 100; i++) {
            ExternalID wExtId1 = new ExternalID();
            wExtId1.setValue("Work Id -" + i + "-" + workUniqueValue);
            wExtId1.setType(WorkExternalIdentifierType.AGR.value());
            wExtId1.setRelationship(Relationship.SELF);
            wExtId1.setUrl(new Url("http://orcid.org/work#" + i + "-" + workUniqueValue));
            work1.getExternalIdentifiers().getExternalIdentifier().add(wExtId1);
        }
        
        work1.getWorkContributors().getContributor().clear();
        for(int i = 0; i < 100; i++) {
            Contributor c = new Contributor();
            ContributorAttributes ca = new ContributorAttributes();
            ca.setContributorRole(ContributorRole.ASSIGNEE);
            ca.setContributorSequence(SequenceType.ADDITIONAL);
            c.setContributorAttributes(ca);
            c.setContributorOrcid(new ContributorOrcid("9999-0000-0000-0000"));
            c.setCreditName(new CreditName("Credit name " + i));
            work1.getWorkContributors().getContributor().add(c);
        }
        
        ClientResponse postResponse = memberV2ApiClient.createWorkXml(this.getUser1OrcidId(), work1, accessToken);
        assertNotNull(postResponse);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String locationPath = postResponse.getLocation().getPath();
        assertTrue("Location header path should match pattern, but was " + locationPath, locationPath.matches(".*/v2.0/" + this.getUser1OrcidId() + "/work/\\d+"));
        return postResponse.getLocation();
    }
    
    private void updateWork(URI workLocation) throws URISyntaxException, InterruptedException, JSONException {
        String accessToken = getAccessToken();
        ClientResponse getResponse = memberV2ApiClient.viewLocationXml(workLocation, accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), getResponse.getStatus());
        Work work = getResponse.getEntity(Work.class);
        work.getWorkTitle().getTitle().setContent(work.getWorkTitle().getTitle().getContent() + "!");
        for(ExternalID extId : work.getExternalIdentifiers().getExternalIdentifier()) {
            extId.setValue(extId.getValue() + "!");
        }
        
        ClientResponse updateResponse = memberV2ApiClient.updateWork(this.getUser1OrcidId(), work, accessToken);
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());        
    }
    
    private void getWorksOn12API() throws InterruptedException, JSONException {
        System.out.println("Viewing works");
        String accessToken = getAccessToken();
        ClientResponse getResponse = t2OAuthClient_1_2.viewFullDetailsXml(this.getUser1OrcidId(), accessToken);
        int status = getResponse.getStatus();
        if(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode() == status) {
            System.out.println("500 error found on server");
            fail();
        }
        
        assertEquals(Response.Status.OK.getStatusCode(), status);
        OrcidMessage orcidMessage = getResponse.getEntity(OrcidMessage.class);
        assertNotNull(orcidMessage);
        assertNotNull(orcidMessage.getOrcidProfile());
        assertNotNull(orcidMessage.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork());
        System.out.println("Works this time: " + orcidMessage.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork().size());
        if(numWorks == -1) {
            numWorks = orcidMessage.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork().size();
        } else {
            assertTrue(orcidMessage.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork().size() > 0);
            numWorks = orcidMessage.getOrcidProfile().getOrcidActivities().getOrcidWorks().getOrcidWork().size();
        }
        
    }
    
    private void deleteWork(Long putCode) throws InterruptedException, JSONException {
        System.out.println("Deleting work " + putCode);
        String accessToken = getAccessToken();
        ClientResponse deleteResponse = memberV2ApiClient.deleteWorkXml(this.getUser1OrcidId(), putCode, accessToken);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }
    
    private String getAccessToken() throws InterruptedException, JSONException {   
        if(accessToken == null) {
            accessToken = getAccessToken(getScopes(ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE));
        }
        return accessToken;
    }
    
}
