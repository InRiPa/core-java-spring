package eu.arrowhead.core.orchestrator.security;

import static org.junit.Assume.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.x509;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.dto.OrchestrationFormRequestDTO;
import eu.arrowhead.common.dto.SystemRequestDTO;

/**
 * IMPORTANT: These tests may fail if the certificates are changed in the src/main/resources folder. 
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
@AutoConfigureMockMvc
public class OrchestratorAccessControlFilterTest {
	
	//=================================================================================================
	// members
	
	private static final String ORCH_ECHO = CommonConstants.ORCHESTRATOR_URI + CommonConstants.ECHO_URI;
	private static final String ORCH_MGMT_STORE_ECHO = CommonConstants.ORCHESTRATOR_URI + CommonConstants.ORCHESTRATOR_STORE_MGMT_URI + CommonConstants.ECHO_URI;
	private static final String ORCH_ORCHESTRATION = CommonConstants.ORCHESTRATOR_URI +CommonConstants.OP_ORCH_PROCESS;
	
	@Autowired
	private ApplicationContext appContext;
	
	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean secure;
	
	@Autowired
	private WebApplicationContext wac;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private MockMvc mockMvc;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@Before
	public void setup() {
		assumeTrue(secure);
		
		final OrchestratorAccessControlFilter orchFilter = appContext.getBean(OrchestratorAccessControlFilter.class);
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac)
									  .apply(springSecurity())
									  .addFilters(orchFilter)
									  .build();
			
		}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEchoCertificateSameCloud() throws Exception {
		this.mockMvc.perform(get(ORCH_ECHO)
				    .secure(true)
					.with(x509("certificates/provider.pem"))
					.accept(MediaType.TEXT_PLAIN))
					.andExpect(status().isOk());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEchoCertificateOtherCloud() throws Exception {
		this.mockMvc.perform(get(ORCH_ECHO)
				    .secure(true)
					.with(x509("certificates/other_cloud.pem"))
					.accept(MediaType.TEXT_PLAIN))
					.andExpect(status().isUnauthorized());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMgmtEchoCertificateSysop() throws Exception {
		this.mockMvc.perform(get(ORCH_MGMT_STORE_ECHO)
				    .secure(true)
					.with(x509("certificates/valid.pem"))
					.accept(MediaType.TEXT_PLAIN))
					.andExpect(status().isOk());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testMgmtEchoCertificateNoSysop() throws Exception {
		this.mockMvc.perform(get(ORCH_MGMT_STORE_ECHO)
				    .secure(true)
					.with(x509("certificates/provider.pem"))
					.accept(MediaType.TEXT_PLAIN))
					.andExpect(status().isUnauthorized());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExternalOrchestrationWithGatekeeper() throws Exception {
		final Map<String, Boolean> flags = new HashMap<>();
		flags.put(CommonConstants.ORCHESTRATON_FLAG_EXTERNAL_SERVICE_REQUEST, true);
		final OrchestrationFormRequestDTO requestDTO = createOrchestrationFromRequestDTO("", flags);
		
		this.mockMvc.perform(post(ORCH_ORCHESTRATION)
				    .secure(true)
					.with(x509("certificates/gatekeeper.pem"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(requestDTO))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest()); //Bad request result means that the request gone through the filter
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExternalOrchestrationWithoutGatekeeper() throws Exception {
		final Map<String, Boolean> flags = new HashMap<>();
		flags.put(CommonConstants.ORCHESTRATON_FLAG_EXTERNAL_SERVICE_REQUEST, true);
		final OrchestrationFormRequestDTO requestDTO = createOrchestrationFromRequestDTO("", flags);
		
		this.mockMvc.perform(post(ORCH_ORCHESTRATION)
				    .secure(true)
					.with(x509("certificates/provider.pem"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(requestDTO))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInternalOrchestrationWithCertificatedSystemName() throws Exception {
		final OrchestrationFormRequestDTO requestDTO = createOrchestrationFromRequestDTO("client-demo-provider", new HashMap<>());
		
		this.mockMvc.perform(post(ORCH_ORCHESTRATION)
				    .secure(true)
					.with(x509("certificates/provider.pem"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(requestDTO))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest()); //Bad request result means that the request gone through the filter
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInternalOrchestrationWithNotCertificatedSystemName() throws Exception {
		final OrchestrationFormRequestDTO requestDTO = createOrchestrationFromRequestDTO("not-certificated-provider", new HashMap<>());
		
		this.mockMvc.perform(post(ORCH_ORCHESTRATION)
				    .secure(true)
					.with(x509("certificates/provider.pem"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsBytes(requestDTO))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
	}
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public OrchestrationFormRequestDTO createOrchestrationFromRequestDTO(final String requesterSystemName, final Map<String, Boolean> flags) {
		final OrchestrationFormRequestDTO dto = new OrchestrationFormRequestDTO();
		
		final SystemRequestDTO requesterSystem = new SystemRequestDTO();
		requesterSystem.setSystemName(requesterSystemName);		
		
		dto.setRequesterSystem(requesterSystem);
		dto.getOrchestrationFlags().putAll(flags);
		
		return dto;
	}
	
}
