package org.info.infobaza.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FetcherMetadata {
    private final String[] types;
    private final String[] sources;
    private final String[] vids;
    private final boolean isActive;
    private final boolean isIncome;
}