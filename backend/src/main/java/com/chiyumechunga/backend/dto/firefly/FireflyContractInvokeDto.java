package com.chiyumechunga.backend.dto.firefly;

import java.util.Map;

/**
 * DTO Payload for invoking a Smart Contract via Firefly.
 * Structure matches the Firefly API expectations:
 * {
 * "location": { "name": "my-chaincode" },
 * "method": { "name": "createAsset" },
 * "input": { "qrHash": "...", "batch": "..."
 *}
 * }
 */
public record FireflyContractInvokeDto(
        Location location,      // Defines which Smart Contract to target
        Method method,          // Defines which function to call
        Map<String, Object> input // The actual arguments for the function
) {

    // Inner record for the "location" object
    public record Location(
            String name // The name of the Chaincode/Contract defined in Firefly
    ) {}

    // Inner record for the "method" object
    public record Method(
            String name // The function name (e.g., "CreateAsset", "TransferCustody")
    ) {}
}