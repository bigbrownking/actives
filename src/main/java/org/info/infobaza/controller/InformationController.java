package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.dto.request.RelativesIncomeRequest;
import org.info.infobaza.dto.request.YearlyCountRequest;
import org.info.infobaza.dto.response.info.active.ActiveResponse;
import org.info.infobaza.dto.response.info.income.IncomeResponse;
import org.info.infobaza.dto.response.info.yearlyCounts.YearlyRecordCounts;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.constants.Dictionary;
import org.info.infobaza.service.history.HistoryService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/info")
public class InformationController {

    private final Analyzer analyzer;
    private final Dictionary dictionary;
    private final HistoryService historyService;

    @LogRequest
    @PostMapping("/active")
    @Cacheable(value = "actives", key = "#request")
    public ResponseEntity<ActiveResponse> getAllActivesOfPerson(@RequestBody RelativesActiveRequest request) {
        ActiveResponse activeResponse = analyzer.getAllActivesOfPersonsByDates(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getYears(),
                request.getVids(),
                request.getTypes(),
                request.getSources(),
                request.getIins()
        );

       // historyService.createRequest(request);
        return ResponseEntity.ok(activeResponse);
    }

    @LogRequest
    @PostMapping("/income")
    @Cacheable(value = "incomes", key = "#request")
    public ResponseEntity<IncomeResponse> getAllIncomesOfPerson(@RequestBody RelativesIncomeRequest request) {
        IncomeResponse incomeResponse = analyzer.getAllIncomesOfPersonsByDates(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getYears(),
                request.getVids(),
                request.getSources(),
                request.getIins()
        );

       // historyService.createRequest(request);
        return ResponseEntity.ok(incomeResponse);
    }

    @LogRequest
    @PostMapping("/yearCount")
    @Cacheable(value = "yearCounts", key = "#request")
    public ResponseEntity<YearlyRecordCounts> getYearlyRecordCounts(@RequestBody YearlyCountRequest request) {
        return ResponseEntity.ok(analyzer.getYearlyRecordCounts(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getSources(),
                request.getTypes(),
                request.getIins(),
                request.isActive()
        ));
    }

    @GetMapping("/sourcesIncome")
    public ResponseEntity<List<String>> getAllSourcesIncome(){
        return ResponseEntity.ok(dictionary.getSourcesIncome());
    }

    @GetMapping("/vidIncome")
    public ResponseEntity<List<String>> getAllVidIncome(){
        return ResponseEntity.ok(dictionary.getVidIncome());
    }


    @GetMapping("/sourcesActive")
    public ResponseEntity<List<String>> getAllSourcesActive(){
        return ResponseEntity.ok(dictionary.getSourcesActive());
    }

    @GetMapping("/typesActive")
    public ResponseEntity<List<String>> getAllTypesActive(){
        return ResponseEntity.ok(dictionary.getTypesActives());
    }

    @GetMapping("/vidActive")
    public ResponseEntity<List<String>> getAllVidActive(){
        return ResponseEntity.ok(dictionary.getVidActive());
    }


}
