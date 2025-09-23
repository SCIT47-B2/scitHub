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
        Page<Company> companyPage = cr.findWithFilters(name, industry, type, locations, pageable);

        // 조회된 엔티티 페이지를 DTO 페이지로 변환하여 반환
        return companyPage.map(CompanyDTO::convertToCompanyDTO);
    }

    /** 지역 대분류명에 해당하는 세부 지역 목록을 반환 */
    public List<String> getLocationsForBroadLocation(String broadLocation) {
        return locationMap.get(broadLocation);
    }

    /**
     * 이름으로 필터링된 초기 회사 목록 조회
     * @param name 검색할 회사명 (null 가능)
     * @return 검색 결과 회사 목록
     */
    public List<CompanyDTO> getFilteredCompanies(String name) {
        List<Company> entityList;

        // 이름(name) 파라미터가 있으면 이름에 포함된 회사만 조회, 없으면 전체 조회
        if (name != null && !name.trim().isEmpty()) {
            entityList = cr.findByNameContaining(name);
        } else {
            entityList = cr.findAll();
        }

        // 엔티티 리스트를 DTO 리스트로 변환
        List<CompanyDTO> dtoList = new ArrayList<>();
        for (Company entity : entityList) {
            dtoList.add(CompanyDTO.convertToCompanyDTO(entity));
        }
        return dtoList;
    }

    /**
     * 주어진 회사 DTO 목록에서 특정 업종에 해당하는 회사만 필터링
     * (참고: 이 방식은 DB에서 모든 데이터를 가져온 후 메모리에서 필터링하므로 성능에 불리할 수 있음)
     * @param allCompanies 필터링할 전체 회사 DTO 목록
     * @param industry     필터링할 업종
     * @return             필터링된 회사 DTO 목록
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
     * 주어진 회사 DTO 목록에서 특정 유형에 해당하는 회사만 필터링
     * (참고: 이 방식은 DB에서 모든 데이터를 가져온 후 메모리에서 필터링하므로 성능에 불리할 수 있음)
     * @param allCompanies 필터링할 전체 회사 DTO 목록
     * @param type         필터링할 유형
     * @return             필터링된 회사 DTO 목록
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

    /**
     * 큰 분류명에 따라 회사 목록 조회
     * @param broadLocation 큰 분류명
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
     * 주어진 회사 DTO 목록을 직원 수(headcount) 순으로 정렬
     * (참고: 이 방식은 메모리에서 정렬하므로, DB에서 정렬하는 것이 더 효율적)
     * @param allCompanies 정렬할 회사 DTO 목록
     * @param order        정렬 방향 ("asc" 또는 "desc")
     * @return             정렬된 회사 DTO 목록
     */
    public List<CompanyDTO> orderByHeadcount(List<CompanyDTO> allCompanies, String order) {
        // Comparator를 사용하여 정렬
        if ("asc".equals(order)) {
            allCompanies.sort((c1,c2) -> Integer.compare(c1.getHeadcount(), c2.getHeadcount()));
        } else {
            allCompanies.sort((c1,c2) -> Integer.compare(c2.getHeadcount(), c1.getHeadcount()));
        }
        return allCompanies;
    }

    /**
     * 주어진 회사 DTO 목록을 평균 평점 순으로 정렬
     * (참고: 이 방식은 메모리에서 정렬하므로, DB에서 정렬하는 것이 더 효율적)
     * @param companyList   정렬할 회사 DTO 목록
     * @param averageRating 정렬 방향 ("asc" 또는 "desc")
     * @return              정렬된 회사 DTO 목록
     */
    public List<CompanyDTO> orderByAverageRating(List<CompanyDTO> companyList, String averageRating) {
        if ("asc".equals(averageRating)) {
            companyList.sort((c1, c2) -> Double.compare(c1.getAverageRating(), c2.getAverageRating()));
        } else {
            companyList.sort((c1, c2) -> Double.compare(c2.getAverageRating(), c1.getAverageRating()));
        }
        return companyList;
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
