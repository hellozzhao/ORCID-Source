package org.orcid.jaxb.model.member_v2;

import java.io.Serializable;

import org.orcid.jaxb.model.common_v2.OrcidIdentifier;

public class Member implements Serializable {
    private static final long serialVersionUID = 6118825812106015220L;
    private MemberType type;
    private OrcidIdentifier orcidIdentifier;
    private String name;
    private String email;
    private String salesforceId;
    public MemberType getType() {
        return type;
    }
    public void setType(MemberType type) {
        this.type = type;
    }
    public OrcidIdentifier getOrcidIdentifier() {
        return orcidIdentifier;
    }
    public void setOrcidIdentifier(OrcidIdentifier orcidIdentifier) {
        this.orcidIdentifier = orcidIdentifier;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getSalesforceId() {
        return salesforceId;
    }
    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }        
}
