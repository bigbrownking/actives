package org.info.infobaza.config.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictor {

    @Scheduled(cron = "0 0 0 1 * ?")
    @CacheEvict(cacheNames = {"actives", "incomes", "yearCounts", "primaryRelations", "secondaryRelations"}, allEntries = true)
    public void evictMonthlyCaches() {
    }
}
