package org.info.infobaza.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.service.Analyzer;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEvictor {
    private final Analyzer analyzer;

    //@Scheduled(cron = "0 0 0 1 * ?")
    @Scheduled(cron = "0 0 */3 * * ?")
    @CacheEvict(cacheNames = {"actives", "incomes", "yearCounts",
             "activeCounts", "primaryRelations", "secondaryRelations"}, allEntries = true)
    public void evictMonthlyCaches() {
        log.info("Deleting old cache...");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clearIinFioCache(){
        analyzer.clearCaches();
    }
}
