package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
    private String title;
    private String content;

    // FullCalendar가 원하는 형식으로 날짜를 포맷시킴
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime start;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime end;
    
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
