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

    //위치 큰 분류명과 세부 지역 매핑하는 맵
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

    public Page<CompanyDTO> findCompanies(String name, Industry industry, CompanyType type, String location, Pageable pageable) {
        List<String> locations = null;
        if (location != null && !location.isEmpty()) {
            locations = locationMap.get(location);
        }

        Page<Company> companyPage = cr.findWithFilters(name, industry, type, locations, pageable);

        return companyPage.map(CompanyDTO::convertToCompanyDTO);
    }

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

    /**
     * 평균 평점 순으로 정렬
     * @param companyList
     * @param averageRating
     * @return
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
     * @return CompanyDTO 객체 (없으면 null)
     */
    @Transactional
    public CompanyDTO selectById(Integer companyId) {
        //findById 메서드는 Optional을 반환하므로 처리 필요
        Optional<Company> companyOptional = cr.findById(companyId);

        return companyOptional.map(CompanyDTO::convertToCompanyDTO).orElse(null);
    }

}
