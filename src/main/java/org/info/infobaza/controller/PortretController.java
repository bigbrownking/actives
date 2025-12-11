package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.request.FindCarRequest;
import org.info.infobaza.dto.request.FindHouseRequest;
import org.info.infobaza.dto.request.GetPortretRequest;
import org.info.infobaza.dto.response.info.car.CarResponse;
import org.info.infobaza.dto.response.info.house.HousePortret;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.security.UserDetailsImpl;
import org.info.infobaza.service.ObjectFinder;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.util.logging.JustificationService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/portret")
public class PortretController {
    private final PortretService portretService;
    private final ObjectFinder objectFinder;
    private final JustificationService justificationService;

    @LogRequest
    @PostMapping("/person")
    public ResponseEntity<Person> getPortret(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody GetPortretRequest byIINRequest) throws IOException, NotFoundException {
        /*if(justificationService.store(
                userDetails,
                Collections.singletonList(byIINRequest.getIin()),
                byIINRequest.getOrderNum(),
                byIINRequest.getApprovement_type(),
                byIINRequest.getCaseNum(),
                byIINRequest.getOrderDate(),
                byIINRequest.getArticleName(),
                byIINRequest.getCheckingName(),
                byIINRequest.getOtherReasons(),
                byIINRequest.getOrganName(),
                byIINRequest.getSphereName(),
                byIINRequest.getTematikName(),
                byIINRequest.getRukName(),
                "Поиск по иин: "
        )){*/
            return ResponseEntity.ok(portretService.getPerson(byIINRequest.getIin()));
      /*  }
        return null;*/
    }

    @LogRequest
    @PostMapping("/car")
    @Cacheable(value = "car", keyGenerator = "requestKeyGenerator")
    public ResponseEntity<CarResponse> getCarInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody FindCarRequest request) throws IOException {
        /*if(justificationService.store(
            userDetails,
            List.of(request.getVin(), request.getGrnz()),
            request.getOrderNum(),
            request.getApprovement_type(),
            request.getCaseNum(),
            request.getOrderDate(),
            request.getArticleName(),
            request.getCheckingName(),
            request.getOtherReasons(),
            request.getOrganName(),
            request.getSphereName(),
            request.getTematikName(),
            request.getRukName(),
            "Поиск по параметрам автомобиля: "
    )){*/
        return ResponseEntity.ok(objectFinder.getCar(request));
   /* }
        return null;*/
    }

    @LogRequest
    @PostMapping("/house")
    @Cacheable(value = "house", keyGenerator = "requestKeyGenerator")
    public ResponseEntity<HousePortret> getHouseInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody FindHouseRequest request) throws IOException {
       /* if(justificationService.store(
                userDetails,
                List.of(request.getRka(), request.getKd()),
                request.getOrderNum(),
                request.getApprovement_type(),
                request.getCaseNum(),
                request.getOrderDate(),
                request.getArticleName(),
                request.getCheckingName(),
                request.getOtherReasons(),
                request.getOrganName(),
                request.getSphereName(),
                request.getTematikName(),
                request.getRukName(),
                "Поиск по параметрам недвижимости: "
        )){*/
            return ResponseEntity.ok(objectFinder.getHouse(request));
      /*  }
        return null;*/
    }
}
