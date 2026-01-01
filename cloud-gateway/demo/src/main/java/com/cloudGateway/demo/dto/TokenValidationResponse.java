package com.cloudGateway.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private int userId;

    public boolean isValid() {
        return valid;
    }

    public int getUserId() {
        return userId;
    }
}
