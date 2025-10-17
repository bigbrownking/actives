package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.MainRequest;
import org.info.infobaza.dto.response.job.*;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.job.PenaltyRecord;
import org.info.infobaza.model.info.job.TurnoverRecord;
import org.info.infobaza.service.adm_shtraf.AdministrationPayService;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.service.enpf.IndustrialService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController {
    private final ENPFService enpfService;
    private final AdministrationPayService administrationPayService;
    private final IndustrialService industrialService;
    private final HeadService headService;


    @LogRequest
    @PostMapping("/pension")
    public ResponseEntity<List<Pension>> getPension(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok(enpfService.getPension(
                mainRequest.getIin_bin(),
                mainRequest.getDateFrom().toString(),
                mainRequest.getDateTo().toString()
        ));
    }

    @LogRequest
    @PostMapping("/head")
    public ResponseEntity<Head> getHead(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok(headService.constructHead(
                mainRequest.getIin_bin(),
                mainRequest.getDateFrom().toString(),
                mainRequest.getDateTo().toString()
        ));
    }

    @LogRequest
    @PostMapping("/industrial")
    public ResponseEntity<Industry> getIndustrial(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok(industrialService.getIndustry(
                mainRequest.getIin_bin()));
    }

    @LogRequest
    @PostMapping("/turnover")
    public ResponseEntity<List<BankTurnoverGroup>> getTurnover(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok(enpfService.getTurnover(
                mainRequest.getIin_bin()
        ));
    }

    @LogRequest
    @PostMapping("/penalty")
    public ResponseEntity<List<PenaltyRecord>> getPenalty(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok(administrationPayService.getAdministrationPay(
                mainRequest.getIin_bin(),
                mainRequest.getDateFrom().toString(),
                mainRequest.getDateTo().toString()
        ));
    }
}
