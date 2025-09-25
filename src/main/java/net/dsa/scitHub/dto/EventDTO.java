package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.schedule.Event;
import net.dsa.scitHub.enums.Visibility;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {
    
    // DTO는 FullCalendar에서 요구하는 대로 이름을 정의해야 함.
    private Integer eventId;
    private Visibility visibility;
    private String userId;

    @NotBlank(message ="予定のタイトルは必須項目です。")
    @Size(max = 150, message = "タイトルの文字数は150字以内です。") // DB: varchar(150)
    private String title;
    
    private String content;

    // FullCalendar가 원하는 형식으로 날짜를 포맷시킴
    @NotNull(message = "開始時間は必須項目です。")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;
    
    @NotNull(message = "終日かどうかの指定は必須です。")
    private Boolean allDay;

    /**
     * Entity -> DTO 변환 메서드
     * @param entity 변환할 eventEntity
     * @return DTO 변환된 eventDTO 객체
     */
    public static EventDTO convertToEventDTO(Event event) {
        return EventDTO.builder()
                        .eventId(event.getEventId())
                        .visibility(event.getVisibility())
                        .userId(event.getUser() != null ? event.getUser().getUsername() : null)
                        .title(event.getTitle())
                        .content(event.getContent())
                        .start(event.getStartAt())
                        .end(event.getEndAt())
                        .allDay(event.getIsAllDay())
                        .build();
    }

}
