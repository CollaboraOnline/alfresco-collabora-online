package dk.magenta.libreoffice.online.service;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;

/**
 * Wrapper around the org.alfresco.service.cmr.security.PersonService.PersonInfo
 * class
 *
 * @author DarkStar1.
 */
public class PersonInfo {
    private final NodeRef nodeRef;
    private final String userName;
    private final String firstName;
    private final String lastName;

    public PersonInfo(NodeRef nodeRef, String userName, String firstName, String lastName) {
        this.nodeRef = nodeRef;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public PersonInfo(PersonService.PersonInfo personInfo) {
        this.nodeRef = personInfo.getNodeRef();
        this.userName = personInfo.getUserName();
        this.firstName = personInfo.getFirstName();
        this.lastName = personInfo.getLastName();
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}