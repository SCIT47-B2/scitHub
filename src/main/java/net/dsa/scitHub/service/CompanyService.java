package net.dsa.scitHub.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

    /**
     * 이름으로 필터링된 초기 회사 목록 조회
     * @param name 검색할 회사명 (null 가능)
     * @return 검색 결과 회사 목록
     */
    public List<CompanyDTO> getFilteredCompanies(String name) {
        List<Company> entityList;

        if (name != null && !name.trim().isEmpty()) {
            entityList = cr.findByNameContaining(name);
        } else {
            entityList = cr.findAll();
        }

        List<CompanyDTO> dtoList = new ArrayList<>();
        for (Company entity : entityList) {
            dtoList.add(CompanyDTO.convertToCompanyDTO(entity));
        }
        return dtoList;
    }

    /**
     * 업종으로 조회 (전체 목록에서 필터링)
     * @param allCompanies 전체 회사 목록
     * @param industry 필터링할 업종
     * @return 필터링된 회사 목록
     */
    public List<CompanyDTO> selectByIndustry(List<CompanyDTO> allCompanies, Industry industry) {

        List<CompanyDTO> filteredList = new ArrayList<>();
        for (CompanyDTO company : allCompanies) {
            if (company.getIndustry() == industry) {
                filteredList.add(company);
            }
        }
        return filteredList;
   }

    /**
     * 유형으로 조회 (전체 목록에서 필터링)
     * @param allCompanies 전체 회사 목록
     * @param type 필터링할 유형
     * @return 필터링된 회사 목록
     */
    public List<CompanyDTO> selectByType(List<CompanyDTO> allCompanies, CompanyType type) {
        
        List<CompanyDTO> filteredList = new ArrayList<>();
        for (CompanyDTO company : allCompanies) {
            if (company.getType() == type) {
                filteredList.add(company);
            }
        }
        return filteredList;
    }


    //위치 큰 분류명과 세부 지역 매핑하는 맵
    private static final Map<String, List<String>> locationMap = new HashMap<>();
    static {
        locationMap.put("칸토", List.of("도쿄", "이바라키", "토치기", "군마", "사이타마", "치바", "카나가와"));
        locationMap.put("홋카이도", List.of("홋카이도", "아오모리", "이와테", "미야기", "아키타", "야마가타", "후쿠시마"));
        locationMap.put("토카이", List.of("아이치", "시즈오카", "기후", "미에"));
        locationMap.put("킨키", List.of("오사카", "교토", "효고", "나라", "와카야마", "시가"));
        locationMap.put("츄고쿠,시코쿠", List.of("톳토리", "시마네", "오카야마", "히로시마", "야마구치", "토쿠시마", "카가와", "에히메", "코치"));
        locationMap.put("큐슈,오키나와", List.of("후쿠오카", "사가", "나가사키", "쿠마모토", "오이타", "미야자키", "가고시마", "오키나와"));
        locationMap.put("호쿠리쿠,코우신에츠", List.of("니가타", "나가노", "야마나시", "도야마", "이시카와", "후쿠이"));
    }

    /**
     * 큰 분류명에 따라 회사 목록 조회
     * @param location 큰 분류명
     * @return 해당 분류에 속한 회사 목록
     */
    public List<CompanyDTO> selectByBroadLocation(String broadLocation) {
        //대분류 이름(broadLocation)으로 세부 지역 리스트를 맵에서 가져옴
        log.debug("Request broad location: {}", broadLocation);
        List<String> locations = locationMap.get(broadLocation);

        //해당 대분류에 매핑된 세부 지역이 없다면 빈 리스트 반환
        if (locations == null || locations.isEmpty()) {
            log.debug("No specific locations found for broad location: {}", broadLocation);
            return new ArrayList<>();
        }
        log.debug("Locations to query: {}", locations);

        //세부 지역 리스트(locations)에 포함된 모든 회사를 조회
        List<Company> entityList = cr.findByLocationIn(locations);
        log.debug("Found {} companies for locations: {}", entityList.size(), locations);

        //DTO 리스트로 변환
        List<CompanyDTO> dtoList = new ArrayList<>();
        for (Company entity : entityList) {
            CompanyDTO dto = CompanyDTO.convertToCompanyDTO(entity);
            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 취업인원 순으로 정렬
     * @param allCompanies 정렬할 회사 목록
     * @param order 정렬 방향 ("asc" / "desc")
     * @return 정렬된 회사 목록
     */
    public List<CompanyDTO> orderByHeadcount(List<CompanyDTO> allCompanies, String order) {

        //정렬 로직 추가
        if ("asc".equals(order)) {
            allCompanies.sort((c1,c2) -> Integer.compare(c1.getHeadcount(), c2.getHeadcount()));
        } else {
            allCompanies.sort((c1,c2) -> Integer.compare(c2.getHeadcount(), c1.getHeadcount()));
        }
        return allCompanies;
    }

}
