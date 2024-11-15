package com.jungle.studybbitback.domain.room.dto.room;

import com.jungle.studybbitback.domain.room.entity.Room;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GetRoomResponseDto {
    private Long id;
    private String name;
    private String roomUrl;
    private String detail;
    private Integer participants;
    private Integer maxParticipants;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Long leaderId;

    public GetRoomResponseDto(Room room) {
        this.id = room.getId();
        this.name = room.getName();
        this.roomUrl = room.getRoomUrl();
        this.detail = room.getDetail();
        this.participants = room.getParticipants();
        this.maxParticipants = room.getMaxParticipants();
        this.profileImageUrl = room.getProfileImageUrl();
        this.createdAt = room.getCreatedAt();
        this.modifiedAt = room.getModifiedAt();
        this.leaderId = room.getLeaderId();
    }
}
