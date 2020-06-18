package eu.arrowhead.core.translator.services.fiware;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.core.translator.services.fiware.common.FiwareEntity;
import eu.arrowhead.core.translator.services.fiware.common.FiwareUrlServices;

import java.util.Map;
import static org.springframework.http.HttpMethod.*;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class FiwareDriver {

    //=================================================================================================
    // members
    private static final Logger logger = LogManager.getLogger(FiwareDriver.class);
    private final FiwareUrlServices fiwareURLservices;
    private final HttpService httpService;
    private final String fiwareHost;
    private final int fiwarePort;

    public FiwareDriver(HttpService httpService, String fiwareHost, int fiwarePort) {
        logger.info(String.format("New FIWARE Driver with Broker host: [%s] port: [%d]", fiwareHost, fiwarePort));
        Assert.notNull(httpService, "No HttpService");
        Assert.notNull(fiwareHost, "No fiwareHost");
        Assert.state(fiwareHost.isBlank(), "No valid fiwareHost");
        Assert.state(fiwareHost.isEmpty(), "No valid fiwareHost");
        Assert.state(fiwarePort == 0, "Port not valid");
        this.httpService = httpService;
        this.fiwareHost = fiwareHost;
        this.fiwarePort = fiwarePort;
        fiwareURLservices = getFiwareServices();
        logger.info(String.format("%18s | %s", "Entities", fiwareURLservices.getEntitiesURL()));
        logger.info(String.format("%18s | %s", "Types", fiwareURLservices.getTypesURL()));
        logger.info(String.format("%18s | %s", "Subscriptions", fiwareURLservices.getSubscriptionsURL()));
        logger.info(String.format("%18s | %s", "Registrations", fiwareURLservices.getRegistrationsURL()));
    }

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------
    private FiwareUrlServices getFiwareServices() {
        return httpService.sendRequest(getUriServices(), GET, FiwareUrlServices.class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final FiwareEntity[] queryEntitiesList() {
        return httpService.sendRequest(getUriEntities(), GET, FiwareEntity[].class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final FiwareEntity[] queryEntitiesList(Map<String, Object> queryParams) {
        return httpService.sendRequest(getUriEntities(queryParams), GET, FiwareEntity[].class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final int createEntity(Map<String, Object> queryParams, FiwareEntity entity) {
        ResponseEntity response = httpService.sendRequest(getUriEntities(queryParams), POST, null, entity);
        return response.getStatusCodeValue();
    }

    //-------------------------------------------------------------------------------------------------
    public final FiwareEntity queryEntity(String entityId, Map<String, Object> queryParams) {
        return httpService.sendRequest(getUriEntities("/" + entityId, queryParams), GET, FiwareEntity.class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final Object retrieveEntityAttributes(String entityId, Map<String, Object> queryParams) {
        return httpService.sendRequest(getUriEntities("/" + entityId + "/attrs", queryParams), GET, Object.class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final int updateOrAppendEntityAttributes(String entityId, Map<String, Object> queryParams, Object attributes) {
        ResponseEntity response = httpService.sendRequest(getUriEntities("/" + entityId + "/attrs", queryParams), POST, null, attributes);
        return response.getStatusCodeValue();
    }

    //-------------------------------------------------------------------------------------------------
    public final int removeEntity(String entityId, Map<String, Object> queryParams) {
        ResponseEntity response = httpService.sendRequest(getUriEntities("/" + entityId, queryParams), DELETE, null);
        return response.getStatusCodeValue();
    }

    //-------------------------------------------------------------------------------------------------
    public final Object[] queryTypesList(Map<String, Object> queryParams) {
        return httpService.sendRequest(getUriTypes(queryParams), GET, Object[].class).getBody();
    }

    //-------------------------------------------------------------------------------------------------
    public final Object retrieveEntityType(String entityType) {
        return httpService.sendRequest(getUriTypes("/" + entityType), GET, Object.class).getBody();
    }

    //=================================================================================================
    // assistant methods
    //-------------------------------------------------------------------------------------------------
    private UriComponentsBuilder getUriBuilder() {
        return UriComponentsBuilder.fromHttpUrl("http://" + fiwareHost).port(fiwarePort);
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriServices() {
        return getUriBuilder().path("/v2").build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriEntities() {
        return getUriBuilder()
                .path(fiwareURLservices.getEntitiesURL())
                .build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriEntities(Map<String, Object> queryParams) {
        return getUriEntities("", queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriEntities(String path, Map<String, Object> queryParams) {
        UriComponentsBuilder uriBuilder = getUriBuilder();
        uriBuilder.path(fiwareURLservices.getEntitiesURL() + path);
        queryParams.entrySet().forEach((entry) -> {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        });
        return uriBuilder.build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriTypes() {
        return getUriBuilder().path(fiwareURLservices.getTypesURL()).build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriTypes(Map<String, Object> queryParams) {
        return getUriEntities("", queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriTypes(String path) {
        UriComponentsBuilder uriBuilder = getUriBuilder();
        uriBuilder.path(fiwareURLservices.getTypesURL() + path);
        return uriBuilder.build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriTypes(String path, Map<String, Object> queryParams) {
        UriComponentsBuilder uriBuilder = getUriBuilder();
        uriBuilder.path(fiwareURLservices.getTypesURL() + path);
        queryParams.entrySet().forEach((entry) -> {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        });
        return uriBuilder.build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriRegistrations() {
        return getUriBuilder().path(fiwareURLservices.getRegistrationsURL()).build();
    }

    //-------------------------------------------------------------------------------------------------
    private UriComponents getUriSubcriptions() {
        return getUriBuilder().path(fiwareURLservices.getSubscriptionsURL()).build();
    }
}
