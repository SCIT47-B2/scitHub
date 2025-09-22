package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.classroom.Classroom;
import net.dsa.scitHub.enums.ClassroomType;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassroomDTO {
    private Integer classroomId;
    private String classroomName;
    private ClassroomType type;
    private Boolean isActive;

    public static ClassroomDTO convertToClassroomDTO(Classroom classroom) {
        return ClassroomDTO.builder()
                .classroomId(classroom.getClassroomId())
                .classroomName(classroom.getName())
                .type(classroom.getType())
                .isActive(classroom.getIsActive())
                .build();
    }
}
