/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.api.memberV2.server.delegator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orcid.api.common.util.ActivityUtils;
import org.orcid.core.exception.ActivityIdentifierValidationException;
import org.orcid.core.exception.OrcidAccessControlException;
import org.orcid.core.exception.OrcidUnauthorizedException;
import org.orcid.core.exception.OrcidVisibilityException;
import org.orcid.core.exception.VisibilityMismatchException;
import org.orcid.core.exception.WrongSourceException;
import org.orcid.core.utils.SecurityContextTestUtils;
import org.orcid.jaxb.model.common_v2.LastModifiedDate;
import org.orcid.jaxb.model.common_v2.Subtitle;
import org.orcid.jaxb.model.common_v2.Title;
import org.orcid.jaxb.model.common_v2.TranslatedTitle;
import org.orcid.jaxb.model.common_v2.Url;
import org.orcid.jaxb.model.common_v2.Visibility;
import org.orcid.jaxb.model.groupid_v2.GroupIdRecord;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.message.WorkExternalIdentifierType;
import org.orcid.jaxb.model.record.summary_v2.ActivitiesSummary;
import org.orcid.jaxb.model.record.summary_v2.WorkGroup;
import org.orcid.jaxb.model.record.summary_v2.WorkSummary;
import org.orcid.jaxb.model.record.summary_v2.Works;
import org.orcid.jaxb.model.record_v2.Address;
import org.orcid.jaxb.model.record_v2.Citation;
import org.orcid.jaxb.model.record_v2.CitationType;
import org.orcid.jaxb.model.record_v2.Education;
import org.orcid.jaxb.model.record_v2.Employment;
import org.orcid.jaxb.model.record_v2.ExternalID;
import org.orcid.jaxb.model.record_v2.ExternalIDs;
import org.orcid.jaxb.model.record_v2.Funding;
import org.orcid.jaxb.model.record_v2.Keyword;
import org.orcid.jaxb.model.record_v2.OtherName;
import org.orcid.jaxb.model.record_v2.PeerReview;
import org.orcid.jaxb.model.record_v2.PersonExternalIdentifier;
import org.orcid.jaxb.model.record_v2.Relationship;
import org.orcid.jaxb.model.record_v2.ResearcherUrl;
import org.orcid.jaxb.model.record_v2.Work;
import org.orcid.jaxb.model.record_v2.WorkBulk;
import org.orcid.jaxb.model.record_v2.WorkTitle;
import org.orcid.jaxb.model.record_v2.WorkType;
import org.orcid.test.DBUnitTest;
import org.orcid.test.OrcidJUnit4ClassRunner;
import org.orcid.test.helper.Utils;
import org.orcid.utils.DateUtils;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OrcidJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:orcid-api-web-context.xml", "classpath:orcid-api-security-context.xml" })
public class MemberV2ApiServiceDelegator_WorksTest extends DBUnitTest {
    protected static final List<String> DATA_FILES = Arrays.asList("/data/EmptyEntityData.xml", "/data/SecurityQuestionEntityData.xml",
            "/data/SourceClientDetailsEntityData.xml", "/data/ProfileEntityData.xml", "/data/WorksEntityData.xml", "/data/ClientDetailsEntityData.xml",
            "/data/Oauth2TokenDetailsData.xml", "/data/OrgsEntityData.xml", "/data/ProfileFundingEntityData.xml", "/data/OrgAffiliationEntityData.xml",
            "/data/PeerReviewEntityData.xml", "/data/GroupIdRecordEntityData.xml", "/data/RecordNameEntityData.xml", "/data/BiographyEntityData.xml");

    // Now on, for any new test, PLAESE USER THIS ORCID ID
    protected final String ORCID = "0000-0000-0000-0003";

    @Resource(name = "memberV2ApiServiceDelegator")
    protected MemberV2ApiServiceDelegator<Education, Employment, PersonExternalIdentifier, Funding, GroupIdRecord, OtherName, PeerReview, ResearcherUrl, Work, WorkBulk, Address, Keyword> serviceDelegator;

    @BeforeClass
    public static void initDBUnitData() throws Exception {
        initDBUnitData(DATA_FILES);
    }

    @AfterClass
    public static void removeDBUnitData() throws Exception {
        Collections.reverse(DATA_FILES);
        removeDBUnitData(DATA_FILES);
    }

    @Test(expected = OrcidUnauthorizedException.class)
    public void testViewWorkWrongToken() {
        SecurityContextTestUtils.setUpSecurityContext("some-other-user", ScopePathType.READ_LIMITED);
        serviceDelegator.viewWork(ORCID, 11L);
    }

    @Test(expected = OrcidUnauthorizedException.class)
    public void testViewWorkSummaryWrongToken() {
        SecurityContextTestUtils.setUpSecurityContext("some-other-user", ScopePathType.READ_LIMITED);
        serviceDelegator.viewWorkSummary(ORCID, 11L);
    }

    @Test
    public void testViewWorkReadPublic() {
        SecurityContextTestUtils.setUpSecurityContextForClientOnly("APP-5555555555555555", ScopePathType.READ_PUBLIC);
        Response r = serviceDelegator.viewWork(ORCID, 11L);
        Work element = (Work) r.getEntity();
        assertNotNull(element);
        Utils.assertIsPublicOrSource(element, "APP-5555555555555555");
    }

    @Test
    public void testViewWorkSummaryReadPublic() {
        SecurityContextTestUtils.setUpSecurityContextForClientOnly("APP-5555555555555555", ScopePathType.READ_PUBLIC);
        Response r = serviceDelegator.viewWorkSummary(ORCID, 11L);
        WorkSummary element = (WorkSummary) r.getEntity();
        assertNotNull(element);
        Utils.assertIsPublicOrSource(element, "APP-5555555555555555");
    }

    @Test
    public void testViewPublicWork() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4446", ScopePathType.READ_LIMITED);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4446", 5L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("Journal article A", work.getWorkTitle().getTitle().getContent());
        assertEquals(Long.valueOf(5), work.getPutCode());
        assertEquals("/4444-4444-4444-4446/work/5", work.getPath());
        assertEquals(WorkType.JOURNAL_ARTICLE, work.getWorkType());
        assertEquals(Visibility.PUBLIC.value(), work.getVisibility().value());
    }

    @Test
    public void testViewLimitedWork() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4446", ScopePathType.READ_LIMITED);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4446", 6L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("Journal article B", work.getWorkTitle().getTitle().getContent());
        assertEquals(Long.valueOf(6), work.getPutCode());
        assertEquals("/4444-4444-4444-4446/work/6", work.getPath());
        assertEquals(WorkType.JOURNAL_ARTICLE, work.getWorkType());
        assertEquals(Visibility.LIMITED.value(), work.getVisibility().value());
    }

    @Test
    public void testViewPrivateWork() {
        // Use the smallest scope in the pyramid to verify that you can read
        // your own limited and protected data
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4446", ScopePathType.READ_LIMITED);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4446", 7L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("Journal article C", work.getWorkTitle().getTitle().getContent());
        assertEquals(Long.valueOf(7), work.getPutCode());
        assertEquals("/4444-4444-4444-4446/work/7", work.getPath());
        assertEquals(WorkType.JOURNAL_ARTICLE, work.getWorkType());
        assertEquals(Visibility.PRIVATE.value(), work.getVisibility().value());
    }

    @Test(expected = OrcidVisibilityException.class)
    public void testViewPrivateWorkYouAreNotTheSourceOf() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4446", ScopePathType.READ_LIMITED);
        serviceDelegator.viewWork("4444-4444-4444-4446", 8L);
        fail();
    }

    @Test(expected = NoResultException.class)
    public void testViewWorkThatDontBelongToTheUser() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4443", ScopePathType.READ_LIMITED);
        serviceDelegator.viewWork("4444-4444-4444-4443", 5L);
        fail();
    }

    @Test
    public void viewWorksTest() {
        SecurityContextTestUtils.setUpSecurityContext(ORCID, ScopePathType.READ_LIMITED);
        Response r = serviceDelegator.viewWorks(ORCID);
        assertNotNull(r);
        Works works = (Works) r.getEntity();
        assertNotNull(works);
        Utils.verifyLastModified(works.getLastModifiedDate());
        assertNotNull(works.getWorkGroup());
        assertNotNull(works.getPath());
        assertEquals(4, works.getWorkGroup().size());
        boolean found1 = false, found2 = false, found3 = false, found4 = false;

        for (WorkGroup workGroup : works.getWorkGroup()) {
            Utils.verifyLastModified(workGroup.getLastModifiedDate());
            assertNotNull(workGroup.getIdentifiers());
            assertNotNull(workGroup.getIdentifiers().getExternalIdentifier());
            assertEquals(1, workGroup.getIdentifiers().getExternalIdentifier().size());
            assertNotNull(workGroup.getWorkSummary());
            assertEquals(1, workGroup.getWorkSummary().size());
            WorkSummary summary = workGroup.getWorkSummary().get(0);
            Utils.verifyLastModified(summary.getLastModifiedDate());
            assertNotNull(summary.getTitle());
            assertNotNull(summary.getTitle().getTitle());
            switch (workGroup.getIdentifiers().getExternalIdentifier().get(0).getValue()) {
            case "1":
                assertEquals("PUBLIC", summary.getTitle().getTitle().getContent());
                assertEquals(Long.valueOf(11), summary.getPutCode());
                found1 = true;
                break;
            case "2":
                assertEquals("LIMITED", summary.getTitle().getTitle().getContent());
                assertEquals(Long.valueOf(12), summary.getPutCode());
                found2 = true;
                break;
            case "3":
                assertEquals("PRIVATE", summary.getTitle().getTitle().getContent());
                assertEquals(Long.valueOf(13), summary.getPutCode());
                found3 = true;
                break;
            case "4":
                assertEquals("SELF LIMITED", summary.getTitle().getTitle().getContent());
                assertEquals(Long.valueOf(14), summary.getPutCode());
                found4 = true;
                break;
            default:
                fail("Invalid external id found: " + workGroup.getIdentifiers().getExternalIdentifier().get(0).getValue());
            }
        }
        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
        assertTrue(found4);
    }

    @Test
    public void testReadPublicScope_Works() {
        SecurityContextTestUtils.setUpSecurityContext(ORCID, ScopePathType.READ_PUBLIC);
        // Public works
        Response r = serviceDelegator.viewWork(ORCID, 11L);
        assertNotNull(r);
        assertEquals(Work.class.getName(), r.getEntity().getClass().getName());

        r = serviceDelegator.viewWorkSummary(ORCID, 11L);
        assertNotNull(r);
        assertEquals(WorkSummary.class.getName(), r.getEntity().getClass().getName());

        // Limited where source is me, should work
        serviceDelegator.viewWork(ORCID, 12L);
        serviceDelegator.viewWorkSummary(ORCID, 12L);

        // Limited with other source should fail
        try {
            serviceDelegator.viewWork(ORCID, 14L);
            fail();
        } catch (OrcidAccessControlException e) {

        } catch (Exception e) {
            fail();
        }

        try {
            serviceDelegator.viewWorkSummary(ORCID, 14L);
            fail();
        } catch (OrcidAccessControlException e) {

        } catch (Exception e) {
            fail();
        }

        // Private where am the source should work
        serviceDelegator.viewWork(ORCID, 13L);
        serviceDelegator.viewWorkSummary(ORCID, 13L);

        // Private with other source should fail
        try {
            serviceDelegator.viewWork(ORCID, 15L);
            fail();
        } catch (OrcidAccessControlException e) {

        } catch (Exception e) {
            fail();
        }

        try {
            serviceDelegator.viewWork(ORCID, 15L);
            fail();
        } catch (OrcidAccessControlException e) {

        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testCleanEmptyFieldsOnWorks() {
        LastModifiedDate lmd = new LastModifiedDate(DateUtils.convertToXMLGregorianCalendar(System.currentTimeMillis()));
        Work work = new Work();
        work.setLastModifiedDate(lmd);
        work.setWorkCitation(new Citation("", CitationType.FORMATTED_UNSPECIFIED));
        WorkTitle title = new WorkTitle();
        title.setTitle(new Title("My Work"));
        title.setSubtitle(new Subtitle("My subtitle"));
        title.setTranslatedTitle(new TranslatedTitle("", ""));
        work.setWorkTitle(title);

        ActivityUtils.cleanEmptyFields(work);

        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertNotNull(work.getWorkTitle().getSubtitle());
        assertEquals("My Work", work.getWorkTitle().getTitle().getContent());
        assertEquals("My subtitle", work.getWorkTitle().getSubtitle().getContent());

        assertNull(work.getWorkCitation());
        assertNull(work.getWorkTitle().getTranslatedTitle());
    }

    @Test
    public void testAddWork() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4445", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewActivities("4444-4444-4444-4445");
        assertNotNull(response);
        ActivitiesSummary summary = (ActivitiesSummary) response.getEntity();
        assertNotNull(summary);
        Utils.verifyLastModified(summary.getLastModifiedDate());
        // Check works
        assertNotNull(summary.getWorks());
        assertNotNull(summary.getWorks().getWorkGroup());
        assertEquals(1, summary.getWorks().getWorkGroup().size());
        Utils.verifyLastModified(summary.getWorks().getLastModifiedDate());
        assertNotNull(summary.getWorks().getWorkGroup().get(0));
        Utils.verifyLastModified(summary.getWorks().getWorkGroup().get(0).getLastModifiedDate());
        assertNotNull(summary.getWorks().getWorkGroup().get(0).getWorkSummary());
        assertEquals(1, summary.getWorks().getWorkGroup().get(0).getWorkSummary().size());

        String title = "work # 1 " + System.currentTimeMillis();
        Work work = Utils.getWork(title);

        response = serviceDelegator.createWork("4444-4444-4444-4445", work);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Long putCode = Utils.getPutCode(response);

        response = serviceDelegator.viewActivities("4444-4444-4444-4445");
        assertNotNull(response);
        summary = (ActivitiesSummary) response.getEntity();
        assertNotNull(summary);
        Utils.verifyLastModified(summary.getLastModifiedDate());
        // Check works
        assertNotNull(summary.getWorks());
        assertNotNull(summary.getWorks().getWorkGroup());
        assertEquals(2, summary.getWorks().getWorkGroup().size());

        boolean haveOld = false;
        boolean haveNew = false;

        for (WorkGroup group : summary.getWorks().getWorkGroup()) {
            Utils.verifyLastModified(group.getLastModifiedDate());
            assertNotNull(group.getWorkSummary());
            assertNotNull(group.getWorkSummary().get(0));
            WorkSummary workSummary = group.getWorkSummary().get(0);
            Utils.verifyLastModified(workSummary.getLastModifiedDate());
            assertNotNull(workSummary.getTitle());
            assertNotNull(workSummary.getTitle().getTitle());
            if ("A Book With Contributors JSON".equals(workSummary.getTitle().getTitle().getContent())) {
                haveOld = true;
            } else if (title.equals(workSummary.getTitle().getTitle().getContent())) {
                haveNew = true;
            }
        }
        assertTrue(haveOld);
        assertTrue(haveNew);
        // Delete them
        serviceDelegator.deleteWork("4444-4444-4444-4445", putCode);
    }

    @Test
    public void testCreateWorksWithBulkAllOK() {
        Long time = System.currentTimeMillis();
        SecurityContextTestUtils.setUpSecurityContext(ORCID, ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);

        WorkBulk bulk = new WorkBulk();
        for (int i = 0; i < 5; i++) {
            Work work = new Work();
            WorkTitle title = new WorkTitle();
            title.setTitle(new Title("Bulk work " + i + " " + time));
            work.setWorkTitle(title);

            ExternalIDs extIds = new ExternalIDs();
            ExternalID extId = new ExternalID();
            extId.setRelationship(Relationship.SELF);
            extId.setType("doi");
            extId.setUrl(new Url("http://doi/" + i + "/" + time));
            extId.setValue("doi-" + i + "-" + time);
            extIds.getExternalIdentifier().add(extId);
            work.setWorkExternalIdentifiers(extIds);

            work.setWorkType(WorkType.BOOK);
            bulk.getBulk().add(work);
        }

        Response response = serviceDelegator.createWorks(ORCID, bulk);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        bulk = (WorkBulk) response.getEntity();
        assertNotNull(bulk);
        assertEquals(5, bulk.getBulk().size());

        for (int i = 0; i < 5; i++) {
            assertTrue(Work.class.isAssignableFrom(bulk.getBulk().get(i).getClass()));
            Work w = (Work) bulk.getBulk().get(i);
            Utils.verifyLastModified(w.getLastModifiedDate());
            assertNotNull(w.getPutCode());
            assertTrue(0L < w.getPutCode());
            assertEquals("Bulk work " + i + " " + time, w.getWorkTitle().getTitle().getContent());
            assertNotNull(w.getExternalIdentifiers().getExternalIdentifier());
            assertEquals("doi-" + i + "-" + time, w.getExternalIdentifiers().getExternalIdentifier().get(0).getValue());

            Response r = serviceDelegator.viewWork(ORCID, w.getPutCode());
            assertNotNull(r);
            assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
            assertEquals("Bulk work " + i + " " + time, ((Work) r.getEntity()).getWorkTitle().getTitle().getContent());

            // Delete the work
            r = serviceDelegator.deleteWork(ORCID, w.getPutCode());
            assertNotNull(r);
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), r.getStatus());
        }
    }

    @Test
    public void testUpdateWork() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4443", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4443", 1L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertEquals(Long.valueOf(1), work.getPutCode());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("A day in the life", work.getWorkTitle().getTitle().getContent());
        assertEquals(WorkType.BOOK, work.getWorkType());
        assertEquals(Visibility.PUBLIC, work.getVisibility());

        work.setWorkType(WorkType.EDITED_BOOK);
        work.getWorkTitle().getTitle().setContent("Updated work title");

        response = serviceDelegator.updateWork("4444-4444-4444-4443", 1L, work);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        response = serviceDelegator.viewWork("4444-4444-4444-4443", 1L);
        assertNotNull(response);
        work = (Work) response.getEntity();
        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertEquals(Long.valueOf(1), work.getPutCode());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("Updated work title", work.getWorkTitle().getTitle().getContent());
        assertEquals(WorkType.EDITED_BOOK, work.getWorkType());

        // Rollback changes so we dont break other tests
        work.setWorkType(WorkType.BOOK);
        work.getWorkTitle().getTitle().setContent("A day in the life");
        response = serviceDelegator.updateWork("4444-4444-4444-4443", 1L, work);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test(expected = WrongSourceException.class)
    public void testUpdateWorkYouAreNotTheSourceOf() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4443", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4443", 2L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertEquals(Long.valueOf(2), work.getPutCode());
        assertNotNull(work.getWorkTitle());
        assertNotNull(work.getWorkTitle().getTitle());
        assertEquals("Another day in the life", work.getWorkTitle().getTitle().getContent());
        assertEquals(WorkType.BOOK, work.getWorkType());

        work.setWorkType(WorkType.EDITED_BOOK);
        work.getWorkTitle().getTitle().setContent("Updated work title");

        ExternalIDs extIds = new ExternalIDs();
        ExternalID extId = new ExternalID();
        extId.setRelationship(Relationship.PART_OF);
        extId.setType(WorkExternalIdentifierType.AGR.value());
        extId.setValue("ext-id-" + System.currentTimeMillis());
        extId.setUrl(new Url("http://thisIsANewUrl.com"));

        extIds.getExternalIdentifier().add(extId);
        work.setWorkExternalIdentifiers(extIds);

        serviceDelegator.updateWork("4444-4444-4444-4443", 2L, work);
        fail();
    }

    @Test(expected = VisibilityMismatchException.class)
    public void testUpdateWorkChangingVisibilityTest() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4445", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4445", 3L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);
        assertEquals(Visibility.LIMITED, work.getVisibility());

        work.setVisibility(Visibility.PRIVATE);

        response = serviceDelegator.updateWork("4444-4444-4444-4445", 3L, work);
        fail();
    }

    @Test
    public void testUpdateWorkLeavingVisibilityNullTest() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4447", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4447", 10L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);
        assertEquals(Visibility.PUBLIC, work.getVisibility());

        work.setVisibility(null);

        response = serviceDelegator.updateWork("4444-4444-4444-4447", 10L, work);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        work = (Work) response.getEntity();
        assertNotNull(work);
        Utils.verifyLastModified(work.getLastModifiedDate());
        assertEquals(Visibility.PUBLIC, work.getVisibility());
    }

    @Test(expected = NoResultException.class)
    public void testDeleteWork() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4447", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Response response = serviceDelegator.viewWork("4444-4444-4444-4447", 9L);
        assertNotNull(response);
        Work work = (Work) response.getEntity();
        assertNotNull(work);

        response = serviceDelegator.deleteWork("4444-4444-4444-4447", 9L);
        assertNotNull(response);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        serviceDelegator.viewWork("4444-4444-4444-4447", 9L);
    }

    @Test
    public void testAddWorkWithInvalidExtIdTypeFail() {
        String orcid = "4444-4444-4444-4499";
        SecurityContextTestUtils.setUpSecurityContext(orcid, ScopePathType.ACTIVITIES_READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        Work work = Utils.getWork("work # 1 " + System.currentTimeMillis());
        try {
            work.getExternalIdentifiers().getExternalIdentifier().get(0).setType("INVALID");
            serviceDelegator.createWork(orcid, work);
            fail();
        } catch (ActivityIdentifierValidationException e) {

        } catch (Exception e) {
            fail();
        }

        // Assert that it could be created with a valid value
        work.getExternalIdentifiers().getExternalIdentifier().get(0).setType("doi");
        Response response = serviceDelegator.createWork(orcid, work);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        Map<?, ?> map = response.getMetadata();
        assertNotNull(map);
        assertTrue(map.containsKey("Location"));
        List<?> resultWithPutCode = (List<?>) map.get("Location");
        Long putCode = Long.valueOf(String.valueOf(resultWithPutCode.get(0)));

        // Delete it to roll back the test data
        response = serviceDelegator.deleteWork(orcid, putCode);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test(expected = WrongSourceException.class)
    public void testDeleteWorkYouAreNotTheSourceOf() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4446", ScopePathType.READ_LIMITED, ScopePathType.ACTIVITIES_UPDATE);
        serviceDelegator.deleteWork("4444-4444-4444-4446", 8L);
        fail();
    }
}
