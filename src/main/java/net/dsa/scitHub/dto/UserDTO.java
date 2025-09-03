package net.dsa.scitHub.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Integer cohortNo;     // select name="cohortNo"
    private String userName;           // input name="userName"
    private String signupPassword;   // input name="signupPassword"
    private String name;             // input name="name"
    private String gender;           // radio name="gender"
    private LocalDate birth;         // input name="birth"
    private String email;            // input name="email"
    private String phone;            // input name="phone"

}
