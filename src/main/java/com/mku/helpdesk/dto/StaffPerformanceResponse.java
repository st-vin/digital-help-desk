package com.mku.helpdesk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceResponse {
    private String staffName;
    private long assignedCount;
    private long resolvedCount;
}
