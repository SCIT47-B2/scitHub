package net.dsa.scitHub.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.AccountDTO;
import net.dsa.scitHub.dto.MenuItem;
import net.dsa.scitHub.enums.Gender;
import net.dsa.scitHub.service.MyPageService;


@Slf4j
@RequiredArgsConstructor
@Controller
public class MypageController {

    private final MyPageService mps;
    // 마이페이지 요청
    @GetMapping("/mypage")
    public String myPage(Model model) {
        List<MenuItem> menuItems = List.of(
            new MenuItem("기본 정보", "/mypage/info"),
            new MenuItem("쪽지함", "/mypage/messages")
        );
        model.addAttribute("menuItems", menuItems);

        // DB 연동 테스트용
        AccountDTO accountDTO = mps.getAccount("Deleted Account");
        model.addAttribute("account", accountDTO);

        return "mypage/info"; // templates/mypage/info.html
    }

    // DB 입력 테스트용
    @GetMapping("/mypage/newAccount")
    public String newFormattedAccount() {
        AccountDTO accountDTO = AccountDTO.builder()
                                .username("testUser")
                                .password("default")
                                .cohortNo(47)
                                .name_kor("고길동")
                                .birthDate(LocalDate.now())
                                .gender(Gender.M)
                                .email("null@null.null")
                                .phone("111-2222-3333")
                                .build();
        mps.makeAccount(accountDTO);

        return "redirect:/";
    }
}
