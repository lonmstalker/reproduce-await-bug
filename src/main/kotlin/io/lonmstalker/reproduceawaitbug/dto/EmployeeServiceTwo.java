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
public class EmployeeServiceTwo {
    private UUID id;
    private String username;
    private String firstName;
    private String secondName;
    private String middleName;
    private Integer age;
    private AnotherData someAnotherData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnotherData {
        private String someAnother;
        private String someAnother1;
        private String someAnother2;
    }
}
