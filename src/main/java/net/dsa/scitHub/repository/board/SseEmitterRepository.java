package net.dsa.scitHub.repository.board;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class SseEmitterRepository {

    private final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(Integer userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.info("SseEmitter 저장됨 [userId={}]", userId);
    }

    public void deleteById(Integer userId) {
        emitters.remove(userId);
        log.info("SseEmitter 삭제됨 [userId={}]", userId);
    }

    public SseEmitter findById(Integer userId) {
        return emitters.get(userId);
    }
}
