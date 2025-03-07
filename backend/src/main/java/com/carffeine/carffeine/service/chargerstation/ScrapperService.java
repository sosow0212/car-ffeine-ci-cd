package com.carffeine.carffeine.service.chargerstation;

import com.carffeine.carffeine.domain.chargestation.ChargeStation;
import com.carffeine.carffeine.domain.chargestation.CustomChargeStationRepository;
import com.carffeine.carffeine.domain.chargestation.charger.Charger;
import com.carffeine.carffeine.domain.chargestation.charger.CustomChargerRepository;
import com.carffeine.carffeine.service.chargerstation.dto.ChargeStationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ScrapperService {

    private static final int THREAD_COUNT = 8;
    private static final int MAX_PAGE_SIZE = 24;
    private static final int MIN_SIZE = 5000;

    private final CustomChargeStationRepository customChargeStationRepository;
    private final ChargeStationRequester chargeStationRequester;
    private final CustomChargerRepository customChargerRepository;

    public void scrap() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 1; i <= MAX_PAGE_SIZE; i++) {
            int page = i;
            executorService.submit(() -> scrapPage(page));
        }
        executorService.shutdown();
    }

    private void scrapPage(int page) {
        try {
            ChargeStationRequest chargeStationRequest = chargeStationRequester.requestChargeStationRequest(page);
            List<ChargeStation> chargeStations = chargeStationRequest.toStations();
            List<Charger> chargers = chargeStationRequest.toChargers();
            if (page != MAX_PAGE_SIZE && chargers.size() < MIN_SIZE) {
                log.error("공공데이터 API 의 사이즈가 이상해요 page: {}, size: {}", page, chargers.size());
            }
            customChargeStationRepository.saveAll(new HashSet<>(chargeStations));
            customChargerRepository.saveAll(chargers);
            log.info("page: {}, size: {} 저장 완료", page, chargers.size());
        } catch (Exception e) {
            log.error("page: {}", page, e);
        }
    }
}
