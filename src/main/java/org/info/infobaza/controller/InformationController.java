package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.constants.Dictionary;
import org.info.infobaza.dto.request.RelativesActiveRequest;
import org.info.infobaza.dto.request.RelativesIncomeRequest;
import org.info.infobaza.dto.response.info.active.ActiveResponse;
import org.info.infobaza.dto.response.info.income.IncomeResponse;
import org.info.infobaza.dto.response.info.yearlyCounts.YearlyRecordCounts;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/info")
public class InformationController {

    private final Analyzer analyzer;
    private final Dictionary dictionary;

    @LogRequest
    @PostMapping("/active")
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

        return ResponseEntity.ok(activeResponse);
    }

    @LogRequest
    @PostMapping("/activeCounts")
    public ResponseEntity<ActiveResponse> getAllActiveCountsOfPerson(@RequestBody RelativesActiveRequest request,
                                                                     @RequestParam(value = "button", required = false) String button) {
        ActiveResponse activeResponse = analyzer.getAllActiveCountsOfPersonsByDates(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getYears(),
                request.getVids(),
                request.getTypes(),
                request.getSources(),
                request.getIins(),
                button
        );

        return ResponseEntity.ok(activeResponse);
    }

    @LogRequest
    @PostMapping("/activeUl")
    public ResponseEntity<ActiveResponse> getAllActiveUlOfPerson(@RequestBody RelativesActiveRequest request,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size){
        ActiveResponse activeResponse = analyzer.getAllActiveUlOfPersonsByDates(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getYears(),
                request.getVids(),
                request.getTypes(),
                request.getSources(),
                request.getIins(),
                page, size
        );

        return ResponseEntity.ok(activeResponse);
    }

    @LogRequest
    @PostMapping("/income")
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

        return ResponseEntity.ok(incomeResponse);
    }

    @LogRequest
    @PostMapping("/yearCount")
    public ResponseEntity<YearlyRecordCounts> getYearlyRecordCounts(@RequestBody RelativesActiveRequest request) {
        return ResponseEntity.ok(analyzer.getYearlyRecordCounts(
                request.getIin(),
                request.getDateFrom().toString(),
                request.getDateTo().toString(),
                request.getSources(),
                request.getTypes(),
                request.getVids(),
                request.getIins(),
                request.isActive()
        ));
    }

    @GetMapping("/sourcesIncome")
    public ResponseEntity<List<String>> getAllSourcesIncome() {
        return ResponseEntity.ok(dictionary.getSourcesIncome());
    }

    @GetMapping("/vidIncome")
    public ResponseEntity<List<String>> getAllVidIncome() {
        return ResponseEntity.ok(dictionary.getVidIncome());
    }


    @GetMapping("/sourcesActive")
    public ResponseEntity<List<String>> getAllSourcesActive() {
        return ResponseEntity.ok(dictionary.getSourcesActive());
    }

    @GetMapping("/typesActive")
    public ResponseEntity<List<String>> getAllTypesActive() {
        return ResponseEntity.ok(dictionary.getTypesActives());
    }

    @GetMapping("/vidActive")
    public ResponseEntity<List<String>> getAllVidActive() {
        return ResponseEntity.ok(dictionary.getVidActive());
    }
}
