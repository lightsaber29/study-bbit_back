package com.jungle.studybbitback.domain.room.entity;

import com.jungle.studybbitback.common.entity.ModifiedTimeEntity;
import com.jungle.studybbitback.domain.room.dto.room.CreateRoomRequestDto;
import com.jungle.studybbitback.domain.room.dto.room.UpdateRoomRequestDto;
import com.jungle.studybbitback.domain.room.entity.roomboard.RoomBoard;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class Room extends ModifiedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    //name, roomUrl, participants,max~는 변경 불가
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String roomUrl;
    private String password;
    private String detail;

    @Column(nullable = false)
    private Integer participants;

    @Column(nullable = false)
    private Integer maxParticipants;

    private String profileImageUrl;

    @Column(nullable = false, name = "leader_id") //leaderId로 쓸 ID임
    private Long leaderId;

    @OneToMany (mappedBy = "room")
    private Set<RoomBoard> roomBoard = new HashSet<>();

    public Room(CreateRoomRequestDto requestDto, Long memberId) {
        this.name = requestDto.getName();
        this.roomUrl = requestDto.getRoomUrl();
        this.password = requestDto.getPassword();
        this.detail = requestDto.getDetail();
        this.participants = 1;
        this.maxParticipants = requestDto.getMaxParticipants();
        this.profileImageUrl = requestDto.getProfileImageUrl();
        this.leaderId = memberId;
    }

    public void updateDetails(UpdateRoomRequestDto requestDto) {
        this.password = requestDto.getPassword();
        this.detail = requestDto.getDetail();
        this.profileImageUrl = requestDto.getProfileImageUrl();
    }

}
