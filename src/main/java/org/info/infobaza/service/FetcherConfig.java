package org.info.infobaza.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.info.infobaza.constants.QueryLocationDictionary;

@Getter
@AllArgsConstructor
public class FetcherConfig {
    private final String[] types;
    private final String source;
    private final String[] vids;
    private final QueryLocationDictionary queryPath;
    private final boolean isActive;
    private final boolean isIncome;
    private final Class<?> returnType;
}