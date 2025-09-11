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
public class MypageDTO {
    private Integer userId;         // hidden name="userId"
    private Integer cohortNo;     // select name="cohortNo"
    private String userName;           // input name="userName"
    private String avatarUrl;       // input name="avatarUrl"
    private String name;             // input name="name"
    private String gender;           // radio name="gender"
    private LocalDate birth;         // input name="birth"
    private String email;            // input name="email"
    private String phone;            // input name="phone"
    private String role;
}
