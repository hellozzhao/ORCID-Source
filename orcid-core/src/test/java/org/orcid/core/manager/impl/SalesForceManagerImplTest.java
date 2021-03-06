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
package org.orcid.core.manager.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.core.manager.EmailManager;
import org.orcid.core.manager.SalesForceManager;
import org.orcid.core.manager.SourceManager;
import org.orcid.core.salesforce.dao.SalesForceDao;
import org.orcid.core.salesforce.model.Contact;
import org.orcid.core.salesforce.model.ContactRole;
import org.orcid.core.salesforce.model.ContactRoleType;
import org.orcid.core.salesforce.model.Member;
import org.orcid.jaxb.model.record_v2.Email;
import org.orcid.jaxb.model.record_v2.Emails;
import org.orcid.persistence.aop.ProfileLastModifiedAspect;
import org.orcid.persistence.dao.SalesForceConnectionDao;
import org.orcid.persistence.jpa.entities.SalesForceConnectionEntity;
import org.orcid.test.TargetProxyHelper;

import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

/**
 * 
 * @author Will Simpson
 *
 */
public class SalesForceManagerImplTest {

    private static final String TEST_ORCID = "4444-4444-4444-4441";

    private SalesForceManager salesForceManager = new SalesForceManagerImpl();

    @Mock
    private SalesForceDao salesForceDao;

    @Mock
    private SourceManager sourceManager;

    @Mock
    private SalesForceConnectionDao salesForceConnectionDao;

    @Mock
    private SelfPopulatingCache salesForceMembersListCache;

    @Mock
    private SelfPopulatingCache salesForceMemberDetailsCache;

    @Mock
    private SelfPopulatingCache salesForceConsortiaListCache;

    @Mock
    private SelfPopulatingCache salesForceConsortiumCache;

    @Mock
    private SelfPopulatingCache salesForceContactsCache;
    
    @Mock
    private EmailManager emailManager;
    
    @Mock
    private ProfileLastModifiedAspect profileLastModifiedAspect;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "emailManager", emailManager);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceDao", salesForceDao);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "sourceManager", sourceManager);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceConnectionDao", salesForceConnectionDao);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceMembersListCache", salesForceMembersListCache);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceMemberDetailsCache", salesForceMemberDetailsCache);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceConsortiaListCache", salesForceConsortiaListCache);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceConsortiumCache", salesForceConsortiumCache);
        TargetProxyHelper.injectIntoProxy(salesForceManager, "salesForceContactsCache", salesForceContactsCache);

        setUpUser();
        setUpContact1();
        setUpContact2();
    }

    private void setUpUser() {
        when(sourceManager.retrieveRealUserOrcid()).thenReturn(TEST_ORCID);
        SalesForceConnectionEntity connection = new SalesForceConnectionEntity();
        connection.setSalesForceAccountId("account1Id");
        when(salesForceConnectionDao.findByOrcid(TEST_ORCID)).thenReturn(connection);
    }

    private void setUpContact1() {
        List<ContactRole> contact1Roles = new ArrayList<>();
        when(salesForceDao.retrieveContactRolesByContactIdAndAccountId("contact1Id", "account1Id")).thenReturn(contact1Roles);
    }

    private void setUpContact2() {
        List<ContactRole> contact2Roles = new ArrayList<>();
        contact2Roles.add(createContactRole("contact2Id", "contact2Idrole1Id", ContactRoleType.MAIN_CONTACT));
        when(salesForceDao.retrieveContactRolesByContactIdAndAccountId("contact2Id", "account1Id")).thenReturn(contact2Roles);
    }

    private void setUpContacts() {
        List<Contact> contacts = new ArrayList<>();
        contacts.add(createContact("id1", "account1Id", "email1@test.orcid.org", "0000-0000-0000-0000"));
        contacts.add(createContact("id2", "account1Id", "email2@test.orcid.org", null));
        contacts.add(createContact("id3", "account1Id", "email3@test.orcid.org", "0000-0000-0000-0001"));
        when(salesForceDao.retrieveAllContactsByAccountId("account1Id")).thenReturn(contacts);
        when(salesForceDao.createContact(any(Contact.class))).thenReturn("id4");
    }
    
    private void setUpEmails() {
        Emails emails = new Emails();
        Email email = new Email();
        email.setEmail("email3@test.orcid.org");
        email.setPrimary(true);
        emails.getEmails().add(email);
        salesForceManager.setProfileLastModifiedAspect(profileLastModifiedAspect);
        when(profileLastModifiedAspect.retrieveLastModifiedDate("0000-0000-0000-0001")).thenReturn(null);
        when(emailManager.getEmails("0000-0000-0000-0001", 0)).thenReturn(emails);
    }
    
    private Contact createContact(String id, String accountId, String email, String orcid) {
        Contact c = new Contact();
        c.setId(id);
        c.setAccountId(accountId);
        c.setEmail(email);
        c.setOrcid(orcid);
        return c;
    }
    
    private ContactRole createContactRole(String contactId, String roleId, ContactRoleType roleType) {
        ContactRole contactRole = new ContactRole();
        contactRole.setId(roleId);
        contactRole.setRoleType(roleType);
        contactRole.setContactId(contactId);
        return contactRole;
    }

    @Test
    public void testUpdateContact2() {
        // Switch from main to technical contact
        Contact contact = new Contact();
        contact.setId("contact2Id");
        contact.setAccountId("account1");
        ContactRole role = new ContactRole(ContactRoleType.TECHNICAL_CONTACT);
        role.setId("contact2Idrole1Id");
        contact.setRole(role);
        salesForceManager.updateContact(contact);
        verify(salesForceDao, times(1)).createContactRole(argThat(r -> {
            return "contact2Id".equals(r.getContactId()) && "account1Id".equals(r.getAccountId()) && ContactRoleType.TECHNICAL_CONTACT.equals(r.getRoleType());
        }));
        verify(salesForceDao, times(1)).removeContactRole(eq("contact2Idrole1Id"));
    }
    
    @Test
    public void createNewContactTest() {
        setUpContacts();
        Contact c1 = new Contact();
        c1.setEmail("new_email@test.orcid.org");
        c1.setAccountId("account1Id");
        salesForceManager.createContact(c1);
        verify(salesForceDao, times(1)).retrieveAllContactsByAccountId("account1Id");
        verify(salesForceDao, times(1)).createContact(c1);
        verify(salesForceDao, times(1)).createContactRole(argThat(a -> {            
            return "id4".equals(a.getContactId()) && ContactRoleType.OTHER_CONTACT.equals(a.getRoleType()) && "account1Id".equals(a.getAccountId());
        }));
    }
    
    @Test
    public void createNewContact_WithExistingEmail_Test() {
        setUpContacts();
        Contact c1 = new Contact();
        c1.setEmail("email1@test.orcid.org");
        c1.setAccountId("account1Id");
        salesForceManager.createContact(c1);
        verify(salesForceDao, times(1)).retrieveAllContactsByAccountId("account1Id");
        verify(salesForceDao, times(0)).createContact(c1);
        verify(salesForceDao, times(1)).createContactRole(argThat(a -> {            
            return "id1".equals(a.getContactId()) && ContactRoleType.OTHER_CONTACT.equals(a.getRoleType()) && "account1Id".equals(a.getAccountId());
        }));
    }
    
    @Test
    public void createNewContact_WithExistingOrcid_Test() {
        setUpEmails();
        setUpContacts();
        Contact c1 = new Contact();
        c1.setOrcid("0000-0000-0000-0001");
        c1.setAccountId("account1Id");
        salesForceManager.createContact(c1);
        verify(salesForceDao, times(1)).retrieveAllContactsByAccountId("account1Id");
        verify(salesForceDao, times(0)).createContact(c1);
        verify(salesForceDao, times(1)).createContactRole(argThat(a -> {            
            return "id3".equals(a.getContactId()) && ContactRoleType.OTHER_CONTACT.equals(a.getRoleType()) && "account1Id".equals(a.getAccountId());
        }));
    }

    @Test
    public void testFindBestWebsiteMatch() throws MalformedURLException {
        List<Member> members = new ArrayList<>();
        members.add(createMember("1", "Account 1", "Account 1 Display", "https://account.com"));
        members.add(createMember("2", "Account 2", "Account 2 Display", "http://www.account.com"));
        members.add(createMember("3", "Account 3", "Account 3 Display", "https://account.com/abc"));
        members.add(createMember("4", "Account 4", "Account 4 Display", "http://www.else.co.uk"));

        assertEquals("1", salesForceManager.findBestWebsiteMatch(new URL("https://account.com"), members).get().getId());
        assertEquals("1", salesForceManager.findBestWebsiteMatch(new URL("http://account.com"), members).get().getId());
        assertEquals("1", salesForceManager.findBestWebsiteMatch(new URL("http://account.com/123?abc"), members).get().getId());
        assertEquals("2", salesForceManager.findBestWebsiteMatch(new URL("http://www.account.com"), members).get().getId());
        assertEquals("2", salesForceManager.findBestWebsiteMatch(new URL("https://www.account.com"), members).get().getId());
        assertEquals("3", salesForceManager.findBestWebsiteMatch(new URL("https://account.com/abc"), members).get().getId());
        assertEquals("4", salesForceManager.findBestWebsiteMatch(new URL("http://else.co.uk"), members).get().getId());
    }

    private Member createMember(String accountId, String name, String publicDisplayName, String website) throws MalformedURLException {
        Member member = new Member();
        member.setId(accountId);
        member.setName(name);
        member.setPublicDisplayName(publicDisplayName);
        member.setWebsiteUrl(new URL(website));
        return member;
    }

}
