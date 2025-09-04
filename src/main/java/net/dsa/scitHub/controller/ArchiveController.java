package net.dsa.scitHub.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import net.dsa.scitHub.dto.CompanyDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.enums.CompanyType;
import net.dsa.scitHub.enums.Industry;
import net.dsa.scitHub.service.CompanyService;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ArchiveController {

    private final CompanyService cs;

    // 아카이브 페이지 요청
    @GetMapping({"/archive", "/archive/companyList"})
    public String archivePage(
            Model model,
            @RequestParam(name="industry", required=false) Industry industry,
            @RequestParam(name="type", required=false) CompanyType type,
            @RequestParam(name="location", required=false) String location,
            @RequestParam(name="headcount", required=false) String headcount,
            @RequestParam(name="name", required=false) String name
        ) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("회사 리뷰", "/archive/companyList"),
            new MenuItem("사진 앨범", "/archive/photoAlbum")
        );
        model.addAttribute("menuItems", menuItems);


        List<CompanyDTO> companyList = cs.getFilteredCompanies(name);


        //업종 필터링
        if (industry != null) {
            companyList = cs.selectByIndustry(companyList, industry);
        }
        //유형 필터링
        if (type != null) {
            companyList = cs.selectByType(companyList, type);
        }
        //위치 필터링
        if (location != null) {
            companyList = cs.selectByBroadLocation(location);
        }
        //취업인원 정렬
        if (headcount != null) {
            companyList = cs.orderByHeadcount(companyList, headcount);
        }


        model.addAttribute("companyList", companyList);

        return "archive/companyList"; // templates/archive/companyList.html
    }


}
