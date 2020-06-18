package eu.arrowhead.core.translator.services.fiware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.CoreSystemRegistrationProperties;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.core.translator.services.fiware.common.FiwareEntity;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Service
public class FiwareService {

    //=================================================================================================
    // members
    private final Logger logger = LogManager.getLogger(FiwareService.class);
    private final Map<String, FiwareEntity> entities = new HashMap<>();
    private FiwareDriver fiwareDriver;
    private ArrowheadDriver arrowheadDriver;

    @Value(CoreCommonConstants.$FIWARE_SERVER_HOST)
    private String fiwareHost;

    @Value(CoreCommonConstants.$FIWARE_SERVER_PORT)
    private int fiwarePort;
    
    @Value(CoreCommonConstants.$SERVER_PORT)
    private int translatorPort;

    @Autowired
    private HttpService httpService;

    @Autowired
    private SSLProperties sslProperties;
    
    @Autowired
    private CoreSystemRegistrationProperties coreSystemRegistrationProperties;

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------
    public void start() {
        logger.info("-- Starting FIWARE Service --");
        logger.info(String.format("Broker host: [%s] port: [%d]", fiwareHost, fiwarePort));
        logger.info("-----------------------------");
    }
    
    
    //-------------------------------------------------------------------------------------------------
    public void unregisterAll() {
        logger.info("-- Custom destroy of FIWARE Service --");
        for (Map.Entry<String,FiwareEntity> entry: entities.entrySet()) {
            getArrowheadDriver().serviceRegistryUnegisterAllServices(entry.getValue());
        }
        logger.info("-----------------------------");
    }

    //-------------------------------------------------------------------------------------------------
    public FiwareEntity[] listEntities(Map<String, Object> queryParams) {
        return getFiwareDriver().queryEntitiesList(queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    public int createEntity(Map<String, Object> queryParams, FiwareEntity entity) {
        return getFiwareDriver().createEntity(queryParams, entity);
    }

    //-------------------------------------------------------------------------------------------------
    public FiwareEntity queryEntity(String entityId, Map<String, Object> queryParams) {
        return getFiwareDriver().queryEntity(entityId, queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    public Object retrieveEntityAttributes(String entityId, Map<String, Object> queryParams) {
        return getFiwareDriver().retrieveEntityAttributes(entityId, queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    public int updateOrAppendEntityAttributes(String entityId, Map<String, Object> queryParams, Object attributes) {
        return getFiwareDriver().updateOrAppendEntityAttributes(entityId, queryParams, attributes);
    }

    //-------------------------------------------------------------------------------------------------
    public int removeEntity(String entityId, Map<String, Object> queryParams) {
        return getFiwareDriver().removeEntity(entityId, queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    public Object[] queryTypesList(Map<String, Object> queryParams) {
        return getFiwareDriver().queryTypesList(queryParams);
    }

    //-------------------------------------------------------------------------------------------------
    public Object retrieveEntityType(String entityType) {
        return getFiwareDriver().retrieveEntityType(entityType);
    }

    //-------------------------------------------------------------------------------------------------
    public Object pluginEntityService(String entityId, String serviceName, String contentType) {
        FiwareEntity entity = entities.get(entityId);
        if (entity == null) {
            throw new ArrowheadException(String.format("No entity with id:%s", entityId), HttpStatus.NOT_FOUND.value());
        }

        Object value = entity.getProperty(serviceName);
        if (value == null) {
            throw new ArrowheadException(String.format("No entity with id:%s and service:%s", entityId, serviceName), HttpStatus.NOT_FOUND.value());
        }

        switch (contentType) {
            case MediaType.APPLICATION_JSON_VALUE:
                return value;

            case "application/senml+json":
                return jsonOject2SenMl(serviceName, value);

            case MediaType.TEXT_PLAIN_VALUE:
                return jsonOject2Text(serviceName, value);

            default:
                throw new ArrowheadException(String.format("Wrong Content Type:%s", contentType), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        }
    }

    //=================================================================================================
    // assistant methods
    //-------------------------------------------------------------------------------------------------
    private FiwareDriver getFiwareDriver() {
        if (fiwareDriver == null) {
            try {
                fiwareDriver = new FiwareDriver(httpService, fiwareHost, fiwarePort);
            } catch(Exception ex) {
                logger.warn("Exception: "+ex.getLocalizedMessage());
            }
        }
        return fiwareDriver;
    }
    
    private ArrowheadDriver getArrowheadDriver() {
        if (arrowheadDriver == null) {
            try {
                arrowheadDriver = new ArrowheadDriver(translatorPort, httpService, sslProperties, coreSystemRegistrationProperties);
            } catch(Exception ex) {
                logger.warn(String.format("%d %s %s %s", translatorPort, httpService == null, sslProperties == null, coreSystemRegistrationProperties == null));
            }
            arrowheadDriver = getArrowheadDriver();
        }
        return arrowheadDriver;
    }
    

    //-------------------------------------------------------------------------------------------------
    private void updateEntities(FiwareEntity[] updatedEntitiesArray) {
        Map<String, FiwareEntity> updatedEntities = new HashMap<>();

        for (FiwareEntity entity : updatedEntitiesArray) {
            updatedEntities.put(entity.getId(), entity);
        }

        Set<String> checkIfUpdated = new HashSet<>(entities.keySet());
        checkIfUpdated.retainAll(updatedEntities.keySet());

        checkIfUpdated.forEach((id) -> {
            if (entities.get(id).equals(updatedEntities.get(id))) {
                logger.debug(String.format("SAME entity id:%s type:%s -> Nothing", id, entities.get(id).getType()));
            } else {
                logger.info(String.format("UPDATED entity id:%s type:%s -> Save changes", id, entities.get(id).getType()));
                entities.replace(id, updatedEntities.get(id));
            }
        });

        Set<String> checkIfNew = new HashSet<>(updatedEntities.keySet());
        checkIfNew.removeAll(entities.keySet());

        checkIfNew.forEach((id) -> {
            logger.info(String.format("NEW entity id:%s type:%s -> AH register", id, updatedEntities.get(id).getType()));
            entities.put(id, updatedEntities.get(id));
            getArrowheadDriver().serviceRegistryRegisterAllServices(updatedEntities.get(id));
        });

        Set<String> checkIfRemoved = new HashSet<>(entities.keySet());
        checkIfRemoved.removeAll(updatedEntities.keySet());

        checkIfRemoved.forEach((id) -> {
            logger.info(String.format("REMOVE entity id:%s type:%s -> AH unregister", id, entities.get(id).getType()));
            FiwareEntity entity = entities.remove(id);
            getArrowheadDriver().serviceRegistryUnegisterAllServices(entity);
        });

    }

    //-------------------------------------------------------------------------------------------------
    private ArrayNode jsonOject2SenMl(String serviceName, Object o) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode senML = mapper.createArrayNode();
        JsonNode json = mapper.valueToTree(o);
        JsonNode sensorValue = mapper.createObjectNode();
        ((ObjectNode) sensorValue).put("n", serviceName);
        ((ObjectNode) sensorValue).put("v", json.has("value")?json.get("value").asText():"");
        JsonNode metadataValue = mapper.createObjectNode();
        ((ObjectNode) metadataValue).put("n", "metadata");
        ((ObjectNode) metadataValue).put("v", json.has("metadata")?json.get("metadata").asText():"");
        senML.addAll(Arrays.asList(sensorValue, metadataValue));
        return senML;
    }

    //-------------------------------------------------------------------------------------------------
    private String jsonOject2Text(String serviceName, Object o) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(o);
        return String.format("%s value is %s", serviceName, json.has("value")?json.get("value").asText():"unknown");
    }

    //=================================================================================================
    // Scheduled methods
    //-------------------------------------------------------------------------------------------------
    @Scheduled(fixedRate = 1000, initialDelay = 10000)
    private void autoBrokerSynchronization() {
        logger.debug("=========== [Synchronization] ===========");
        updateEntities(getFiwareDriver().queryEntitiesList());
        logger.debug("=========================================");
    }

}
