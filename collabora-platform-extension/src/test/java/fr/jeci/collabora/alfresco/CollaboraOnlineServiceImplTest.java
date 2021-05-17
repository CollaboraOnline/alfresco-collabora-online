package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.impl.AllowPermissionServiceImpl;
import org.alfresco.repo.version.NodeServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import fr.jeci.collabora.alfresco.WopiDiscovery.DiscoveryAction;

@Transactional
public class CollaboraOnlineServiceImplTest {
	private static Log logger = LogFactory.getLog(CollaboraOnlineServiceImplTest.class);

	/**
	 * version store node service
	 */
	protected CollaboraOnlineServiceImpl collaboraOnlineService = null;

	NodeRef nodeRef = null;
	NodeService nodeService = null;

	static String LOCALHOST_SERVER = "http://localhost:8080/";
	static String PUBLICHOST_SERVER = "https://my.server.demo.com/";

	@Before
	public void setUp() throws Exception {
		nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "test-collabora");

		this.collaboraOnlineService = new CollaboraOnlineServiceImpl();

		nodeService = mock(NodeServiceImpl.class);
		this.collaboraOnlineService.setNodeService(nodeService);
		this.collaboraOnlineService.setPermissionService(mock(AllowPermissionServiceImpl.class));

		this.collaboraOnlineService.setCollaboraPublicUrl(new URL(PUBLICHOST_SERVER));
		this.collaboraOnlineService.setTokenTtlMs(1000);
		this.collaboraOnlineService.setAlfrescoPrivateURL(new URL(LOCALHOST_SERVER));
		this.collaboraOnlineService.setAlfrescoPublicURL(new URL(PUBLICHOST_SERVER));

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetWopiSrcURL() throws IOException {
		when(nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)).thenReturn("toto.ods");

		WopiDiscovery wopiDiscovery = mock(WopiDiscovery.class);
		wopiDiscovery.setCollaboraPrivateUrl(new URL(LOCALHOST_SERVER));

		List<DiscoveryAction> actions = new ArrayList<>();
		String urlsrc = PUBLICHOST_SERVER + "/loleaflet/1430151/loleaflet.html?";
		DiscoveryAction actionOds = wopiDiscovery.new DiscoveryAction("ods", "edit", urlsrc);
		actions.add(actionOds);
		when(wopiDiscovery.getAction("ods")).thenReturn(actions);
		this.collaboraOnlineService.setWopiDiscovery(wopiDiscovery);

		// Create a new versionable node

		String wopiSrcURL = this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit");
		assertEquals(urlsrc, wopiSrcURL);
	}

}
