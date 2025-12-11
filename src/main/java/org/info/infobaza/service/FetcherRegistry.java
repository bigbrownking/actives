package org.info.infobaza.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FetcherRegistry {
    private final Map<String, List<DataFetcher>> activeFetchersBySource = new HashMap<>();
    private final Map<String, List<DataFetcher>> incomeFetchersBySource = new HashMap<>();

    public FetcherRegistry(List<DataFetcher> allFetchers) {
        for (DataFetcher fetcher : allFetchers) {
            FetcherMetadata meta = fetcher.getMetadata();
            for (String source : meta.getSources()) {
                if (meta.isActive()) {
                    activeFetchersBySource
                            .computeIfAbsent(source, k -> new ArrayList<>())
                            .add(fetcher);
                }
                if (meta.isIncome()) {
                    incomeFetchersBySource
                            .computeIfAbsent(source, k -> new ArrayList<>())
                            .add(fetcher);
                }
            }
        }
    }

    public List<DataFetcher> getActiveFetchers(String source) {
        return activeFetchersBySource.getOrDefault(source, List.of());
    }

    public List<DataFetcher> getIncomeFetchers(String source) {
        return incomeFetchersBySource.getOrDefault(source, List.of());
    }
    public Map<String, List<DataFetcher>> getAllActiveFetchersBySource() {
        return Collections.unmodifiableMap(activeFetchersBySource);
    }

    public Map<String, List<DataFetcher>> getAllIncomeFetchersBySource() {
        return Collections.unmodifiableMap(incomeFetchersBySource);
    }

    public Set<String> getAllActiveSources() {
        return Collections.unmodifiableSet(activeFetchersBySource.keySet());
    }

    public Set<String> getAllIncomeSources() {
        return Collections.unmodifiableSet(incomeFetchersBySource.keySet());
    }
}
