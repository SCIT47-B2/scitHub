package net.dsa.scitHub.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.user.User;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserManageDTO {
    private Integer userId;
    private String username;
    private String nameKor;
    private Integer cohortNo;
    private LocalDate birthDate;
    private String email;
    private String gender;
    private String phone;
    private Boolean isActive;
    private String role;

    public static UserManageDTO convertToUserDTO(User user) {
        return UserManageDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nameKor(user.getNameKor())
                .cohortNo(user.getCohortNo())
                .birthDate(user.getBirthDate())
                .email(user.getEmail())
                .gender(user.getGender().getDisplayName())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .role(user.getRole().getDisplayName())
                .build();
    }
}
