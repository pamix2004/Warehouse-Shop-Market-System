package com.politechnika.warehouseManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenValidationResponse {

    private boolean valid;
    private int userId;

    public TokenValidationResponse(boolean valid, int userId) {
        this.valid = valid;
        this.userId = userId;
    }

    public boolean isValid() {
        return valid;
    }

    public int getUserId() {
        return userId;
    }
}