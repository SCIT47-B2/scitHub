package net.dsa.scitHub.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.enums.Gender;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDTO {
    
    public String username;

    public String password;

    public int cohortNo;

    public String name_kor;

    public Gender gender;

    public LocalDate birthDate;

    public String email;

    public String phone;
}
