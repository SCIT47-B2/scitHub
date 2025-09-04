package net.dsa.scitHub.service;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.repository.board.BoardRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BoardService {
    private final BoardRepository br;

    public int getBoardIdFromName(String boardName) {
        return br.existsByName(boardName) ? br.findByName(boardName).get().getBoardId() : -1;
    }

}
