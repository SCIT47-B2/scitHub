package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.CompanyDTO;
import net.dsa.scitHub.entity.company.Company;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;
import net.dsa.scitHub.repository.company.CompanyRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CompanyService {

    @Autowired
    private CompanyRepository cr;

    // 위치 대분류명과 세부 지역 목록을 매핑하는 맵
    private static final Map<String, List<String>> locationMap = new HashMap<>();
    static {
        locationMap.put("関東", List.of("東京", "茨城", "栃木", "群馬", "埼玉", "千葉", "神奈川"));
        locationMap.put("北海道", List.of("北海道", "青森", "岩手", "宮城", "秋田", "山形", "福島"));
        locationMap.put("東海", List.of("愛知", "静岡", "岐阜", "三重"));
        locationMap.put("近畿", List.of("大阪", "京都", "兵庫", "奈良", "和歌山", "滋賀"));
        locationMap.put("中国・四国", List.of("鳥取", "島根", "岡山", "広島", "山口", "徳島", "香川", "愛媛", "高知"));
        locationMap.put("九州・沖縄", List.of("福岡", "佐賀", "長崎", "熊本", "大分", "宮崎", "鹿児島", "沖縄"));
        locationMap.put("北陸・甲信越", List.of("新潟", "長野", "山梨", "富山", "石川", "福井"));
    }

    /**
     * 다양한 조건으로 회사를 필터링하고 페이징하여 조회
     * @param name       검색할 회사 이름
     * @param industry   필터링할 업종
     * @param type       필터링할 회사 유형
     * @param location   필터링할 지역 (대분류)
     * @param pageable   페이징 및 정렬 정보
     * @return           페이징된 회사 DTO 목록
     */
    public Page<CompanyDTO> findCompanies(String name, Industry industry, CompanyType type, String location, Pageable pageable) {
        // 지역 대분류(location)가 주어진 경우, 해당하는 세부 지역 목록을 가져옴
        List<String> locations = null;
        if (location != null && !location.isEmpty()) {
            locations = locationMap.get(location);
        }

        // Repository를 호출하여 조건에 맞는 회사 엔티티 페이지를 조회
        Page<Object[]> resultPage = cr.findWithFilters(name, industry, type, locations, pageable);

        // 조회된 엔티티 페이지를 DTO 페이지로 변환하여 반환
        // Object[]의 첫 번째 요소는 Company 엔티티.
        return resultPage.map(result -> CompanyDTO.convertToCompanyDTO((Company) result[0]));
    }

    /**
     * ID로 회사 조회.
     * @param companyId 조회할 회사 ID
     * @return 조회된 CompanyDTO 객체. 해당 ID의 회사가 없으면 null을 반환
     */
    @Transactional
    public CompanyDTO selectById(Integer companyId) {
        // findById는 Optional<Company>를 반환하므로, orElse(null) 등으로 처리 필요
        Optional<Company> companyOptional = cr.findById(companyId);

        // Optional에 회사가 존재하면 DTO로 변환하고, 없으면 null을 반환
        return companyOptional.map(CompanyDTO::convertToCompanyDTO).orElse(null);
    }

}
