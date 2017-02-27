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
package org.orcid.pojo.ajaxForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.orcid.jaxb.model.common_v2.OrcidIdentifier;
import org.orcid.jaxb.model.member_v2.MemberType;
import org.orcid.persistence.jpa.entities.ProfileEntity;

public class Member implements ErrorsInterface, Serializable {
    private static final long serialVersionUID = 1L;

    private List<String> errors = new ArrayList<String>();
    private Text type;
    private Text groupOrcid;
    private Text groupName;
    private Text email;
    private Text salesforceId;
    private List<Client> clients = new ArrayList<Client>();

    @Override
    public List<String> getErrors() {
        return errors;
    }

    @Override
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public static Member fromProfileEntity(ProfileEntity profile){
    	Member group = new Member();
    	group.setEmail(Text.valueOf(profile.getPrimaryEmail().getId()));
    	
    	if(profile.getRecordNameEntity() != null) {
    	    group.setGroupName(Text.valueOf(profile.getRecordNameEntity().getCreditName()));
    	} 
    	
    	group.setGroupOrcid(Text.valueOf(profile.getId()));
    	group.setType(Text.valueOf(profile.getGroupType().value()));
    	group.setSalesforceId(Text.valueOf(profile.getSalesforeId()));
    	return group;
    }
    
    public org.orcid.jaxb.model.member_v2.Member toMember() {
        org.orcid.jaxb.model.member_v2.Member member = new org.orcid.jaxb.model.member_v2.Member();
        if(!PojoUtil.isEmpty(this.getEmail())) {
            member.setEmail(this.getEmail().getValue());
        }
        
        if(!PojoUtil.isEmpty(this.getGroupName())) {
            member.setName(this.getGroupName().getValue());    
        }
        
        if(!PojoUtil.isEmpty(this.getGroupOrcid())) {
            member.setOrcidIdentifier(new OrcidIdentifier(this.getGroupOrcid().getValue()));
        }
        
        if(!PojoUtil.isEmpty(this.getSalesforceId())) {
            member.setSalesforceId(this.getSalesforceId().getValue());            
        }
        //Type must not be null
        member.setType(MemberType.fromValue(this.getType().getValue()));
        return member;
    }
    
    public static Member valueOf(org.orcid.jaxb.model.member_v2.Member member) {
        Member result = new Member();
        result.setEmail(Text.valueOf(member.getEmail()));
        result.setGroupName(Text.valueOf(member.getName()));
        result.setGroupOrcid(Text.valueOf(member.getOrcidIdentifier().getPath()));
        result.setType(Text.valueOf(member.getType().value()));
        if(member.getSalesforceId() != null)
            result.setSalesforceId(Text.valueOf(member.getSalesforceId()));            
        return result;
    }
    
    public Text getType() {
        return type;
    }

    public void setType(Text type) {
        this.type = type;
    }

    public Text getGroupOrcid() {
        return groupOrcid;
    }

    public void setGroupOrcid(Text groupOrcid) {
        this.groupOrcid = groupOrcid;
    }

    public Text getGroupName() {
        return groupName;
    }

    public void setGroupName(Text groupName) {
        this.groupName = groupName;
    }

    public Text getEmail() {
        return email;
    }

    public void setEmail(Text email) {
        this.email = email;
    }

    public Text getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(Text salesforceId) {
        this.salesforceId = salesforceId;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }            
}
