package org.info.infobaza.config.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CacheEvictor {

    @Scheduled(cron = "0 0 0 1 * ?")
    @CacheEvict(cacheNames = {"actives", "incomes", "yearCounts", "primaryRelations", "secondaryRelations"}, allEntries = true)
    public void evictMonthlyCaches() {
        log.info("Deleting old cache...");
    }
}
