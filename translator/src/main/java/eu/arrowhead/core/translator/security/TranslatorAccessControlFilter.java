package eu.arrowhead.core.translator.security;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.security.CoreSystemAccessControlFilter;

@Component
@ConditionalOnProperty(name = CommonConstants.SERVER_SSL_ENABLED, matchIfMissing = true)
public class TranslatorAccessControlFilter extends CoreSystemAccessControlFilter {

    //=================================================================================================
    // members
    //=================================================================================================
    // assistant methods
    //-------------------------------------------------------------------------------------------------
    @Override
    protected void checkClientAuthorized(final String clientCN, final String method, final String requestTarget, final String requestJSON, final Map<String, String[]> queryParams) {
        super.checkClientAuthorized(clientCN, method, requestTarget, requestJSON, queryParams);
        // TODO: implement translator specific logic here
    }
}
