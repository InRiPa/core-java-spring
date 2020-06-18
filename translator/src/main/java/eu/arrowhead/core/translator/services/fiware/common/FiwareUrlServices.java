package eu.arrowhead.core.translator.services.fiware.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FiwareUrlServices {

    //=================================================================================================
    // members
    private final String entities_url;
    private final String types_url;
    private final String subscriptions_url;
    private final String registrations_url;

    @JsonCreator
    public FiwareUrlServices(
            @JsonProperty("entities_url") String entities_url,
            @JsonProperty("types_url") String types_url,
            @JsonProperty("subscriptions_url") String subscriptions_url,
            @JsonProperty("registrations_url") String registrations_url
    ) {
        this.entities_url = entities_url;
        this.types_url = types_url;
        this.subscriptions_url = subscriptions_url;
        this.registrations_url = registrations_url;
    }

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------
    public String getEntitiesURL() {
        return entities_url;
    }

    //-------------------------------------------------------------------------------------------------
    public String getTypesURL() {
        return types_url;
    }

    //-------------------------------------------------------------------------------------------------
    public String getSubscriptionsURL() {
        return subscriptions_url;
    }

    //-------------------------------------------------------------------------------------------------
    public String getRegistrationsURL() {
        return registrations_url;
    }

}
