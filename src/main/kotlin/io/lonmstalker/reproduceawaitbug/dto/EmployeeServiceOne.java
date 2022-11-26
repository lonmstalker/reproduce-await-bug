package io.lonmstalker.reproduceawaitbug.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

// in real project, this exists in another java service and call from my kotlin service
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeServiceOne {
    private UUID id;
    private String username;
    private String firstName;
    private String secondName;
    private String middleName;
    private Integer age;
}
