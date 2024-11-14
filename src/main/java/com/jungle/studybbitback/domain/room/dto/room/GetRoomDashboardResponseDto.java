package com.jungle.studybbitback.domain.room.dto.room;

import com.jungle.studybbitback.domain.room.entity.Room;
import lombok.Getter;

@Getter
public class GetRoomDashboardResponseDto {
    private Long id;
    private String name;
    private String detail;
    private Integer participants;
    private Integer maxParticipants;
    private Boolean isMeetingActive;
    private Long leaderId;

    public GetRoomDashboardResponseDto(Room room) {
        this.id = room.getId();
        this.name = room.getName();
        this.detail = room.getDetail();
        this.participants = room.getParticipants();
        this.maxParticipants = room.getMaxParticipants();
        this.isMeetingActive = room.getMeetingId() != null;
        this.leaderId = room.getLeaderId();
    }
}
