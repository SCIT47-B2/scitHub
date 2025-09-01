package net.dsa.scitHub.service;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.AccountDTO;
import net.dsa.scitHub.entity.user.Account;
import net.dsa.scitHub.repository.user.AccountRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MyPageService {
    private final AccountRepository ar;
    /** DB 연동 테스트용 */
    public AccountDTO getAccount(String username) {
        log.debug("Input user ID : {}", username);

        Account account = ar.findByUsername(username).orElseThrow(
            () -> new EntityNotFoundException("존재하지 않는 ID")
        );

        log.debug("found entity : {}", account);

        AccountDTO accountDTO = AccountDTO.builder()
                                .username(account.getUsername())
                                .build();
        return accountDTO;
    }

    /** DB 연동 테스트용 */
    public void makeAccount(AccountDTO accountDTO) {
        log.debug("accountDTO : {}", accountDTO);
        
        Account newAccount = Account.builder()
                             .username(accountDTO.getUsername())
                             .passwordHash(accountDTO.getPassword())
                             .cohortNo(accountDTO.getCohortNo())
                             .nameKor(accountDTO.getName_kor())
                             .birthDate(accountDTO.getBirthDate())
                             .gender(accountDTO.getGender())
                             .email(accountDTO.getEmail())
                             .phone(accountDTO.getPhone())
                             .build();
        ar.save(newAccount);
    }
}
