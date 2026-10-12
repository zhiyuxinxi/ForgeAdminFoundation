package com.mdframe.forge.flow.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;

/** Missing or malformed post-start responses cannot prove that the remote transaction failed. */
final class FlowStartResponseReader {
    private FlowStartResponseReader() {
    }

    static FlowResult<String> read(ObjectMapper json, ResponseEntity<String> response) throws JsonProcessingException {
        if (response == null || !response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null || response.getBody().isBlank()) {
            throw new IllegalStateException("FLOW_START_RESPONSE_MISSING");
        }
        FlowResult<String> result = json.readValue(response.getBody(), new TypeReference<FlowResult<String>>() { });
        if (result == null || result.getCode() == null
                || (result.isSuccess() && (result.getData() == null || result.getData().isBlank()))) {
            throw new IllegalStateException("FLOW_START_RESPONSE_INVALID");
        }
        return result;
    }
}
