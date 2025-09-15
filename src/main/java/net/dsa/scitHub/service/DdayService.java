package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.DdayDTO;
import net.dsa.scitHub.entity.schedule.Dday;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.repository.schedule.DdayRepository;
import net.dsa.scitHub.repository.user.UserRepository;
import net.dsa.scitHub.security.AuthenticatedUser;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DdayService {
    
    // DB 저장을 위한 Repository 주입
    private final DdayRepository dr;
    private final UserRepository ur;

    /**
     * dday를 등록하는 함수
     * @param DdayDTO form에서 받은 정보를 담고 있는 DTO, 여기서는 title과 dday
     * @param userDetails 로그인 정보를 가지고 있었던 객체
     * @return DdayDTO 넘어온 정보에 로그인 정보를 합친 DTO
     */
    public DdayDTO createDday(DdayDTO ddayDTO, AuthenticatedUser userDetails) {
        
        // Entity에 넣기 위해 User객체를 생성
        User user = ur.findByUsername(userDetails.getId()).orElseThrow(() -> new EntityNotFoundException("아이디가 없습니다."));
        
        // 로그인 정보와 form에서 받은 정보로 Dday Entity 만듦
        Dday ddayEntity = Dday.builder()
                            .user(user)
                            .dday(ddayDTO.getDday())
                            .title(ddayDTO.getTitle())
                            .build();

        // DB에 저장하고 ddayId까지 있는 완전한 Entity를 저장
        Dday newDdayEntity = dr.save(ddayEntity);
        
        // Entity를 DTO로 변환
        DdayDTO newDdayDTO = DdayDTO.convertToDdayDTO(newDdayEntity);

        // 새로 저장된 dday의 정보를 담고 있는 DTO 반환
        return newDdayDTO; 

    }

    /**
     * 특정 사용자의 모든 디데이 목록을 조회하는 메서드
     */
    public List<DdayDTO> findAllDdays(AuthenticatedUser userDetails) {
        // 인증 정보로 User Entity 조회
        User user = ur.findByUsername(userDetails.getUsername()).orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        
        // 해당 User Entity와 연관된 모든 Dday Entity 목록을 DB에서 조회
            // 새로 Repository에서 정의된 조회 함수 사용
        List<Dday> ddays = dr.findByUserOrderByDdayAsc(user);

        // List<DdayDTO> 객체 준비
        List<DdayDTO> ddayDTOs = new ArrayList<>();

        // 하나씩 꺼내서 DdayDTO로 변환시키고 리스트에 add
        for(Dday dday : ddays) {
            DdayDTO ddayDTO = DdayDTO.convertToDdayDTO(dday);
            
            ddayDTOs.add(ddayDTO);
        }

        return ddayDTOs;
    }

    /**
     * dday 삭제하는 함수
     * @param userDetails 로그인 정보
     * @param {Integer} ddayId 삭제할 ddayId
     */
    public void deleteDday(AuthenticatedUser userDetails, Integer ddayId) {
        // 삭제하고자 하는 dday의 Entity
        Dday ddayEntity = dr.findById(ddayId).orElseThrow(() -> new EntityNotFoundException("찾고자 하는 디데이가 없습니다."));

        // 디데이의 username과 로그인한 사용자의 name이 일치하는지 확인
        if (!ddayEntity.getUser().getUsername().equals(userDetails.getId())) {
            // 일치하지 않으면 접근 거부 예외를 발생
            throw new AccessDeniedException("이 디데이를 삭제할 권한이 없습니다.");
        }

        // dday 삭제
        dr.deleteById(ddayId);
    }

    /**
     * dday 수정하는 함수
     * @param userDetails 로그인 정보
     * @param {Integer} ddayId 수정할 ddayId
     * @param {DdayDTO} ddayDTO 수정할 내용이 담긴 DTO
     * @request {DdayDTO} updatedDTO 업데이트된 DTO
     */
    public DdayDTO updateDday(AuthenticatedUser userDetails, Integer ddayId, DdayDTO ddayDTO) {
        // 수정하고자 하는 dday가 있는지 확인, 없으면 에러 발생
        Dday ddayEntity = dr.findById(ddayId).orElseThrow(() -> new EntityNotFoundException("찾는 dday가 없습니다."));

        // 디데이의 username과 로그인한 사용자의 name이 일치하는지 확인
        if (!ddayEntity.getUser().getUsername().equals(userDetails.getId())) {
            // 일치하지 않으면 접근 거부 예외를 발생
            throw new AccessDeniedException("이 디데이를 수정할 권한이 없습니다.");
        }

        // 수정할 정보가 담긴 DTO에서 Entity로 값을 넣어줌.
        ddayEntity.setTitle(ddayDTO.getTitle());
        ddayEntity.setDday(ddayDTO.getDday());

        // JPA가 자동저장해준 Entity를 사용하여 DTO로 변환하여 return
        return DdayDTO.convertToDdayDTO(ddayEntity);
    }

}
