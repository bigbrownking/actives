package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.request.FindCarRequest;
import org.info.infobaza.dto.request.FindHouseRequest;
import org.info.infobaza.dto.response.info.car.*;
import org.info.infobaza.dto.response.info.house.*;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.model.info.object.Car;
import org.info.infobaza.model.info.object.CarInsurance;
import org.info.infobaza.model.info.object.CarInsuranceSummary;
import org.info.infobaza.service.cars.CarService;
import org.info.infobaza.service.gkb_auto.GKBAUTOService;
import org.info.infobaza.service.kap_mvd_auto.KAPMVDAUTOService;
import org.info.infobaza.service.nao_con.NaoConService;
import org.info.infobaza.service.portret.PortretService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ObjectFinder {
    private final NaoConService naoConService;
    private final PortretService portretService;
    private final CarService carService;

    public HousePortret getHouse(FindHouseRequest request) throws IOException {
        // ----------- Case 1: Search by KD or RKA -----------
        if (request.getKd() != null || request.getRka() != null) {
            List<NaoConRecordDt> records = naoConService.searchNaoByKdRka(request.getKd(), request.getRka());

            if (records == null || records.isEmpty()) {
                return HousePortret.builder()
                        .kd("Не найдено")
                        .rka("Не найдено")
                        .volume("Не найдено")
                        .currentOwner("Не найдено")
                        .portrets(List.of())
                        .build();
            }

            records.sort(Comparator.comparing(NaoConRecordDt::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            List<HouseHistory> history = records.stream()
                    .map(record -> HouseHistory.builder()
                            .seller(record.getIin_bin_prod())
                            .buyer(record.getIin_bin_pokup())
                            .date(record.getDate())
                            .summ(record.getSumm())
                            .build())
                    .distinct()
                    .toList();

            NaoConRecordDt last = records.get(records.size() - 1);

            Set<String> sellerIins = history.stream()
                    .filter(h -> h.getSeller() != null)
                    .map(HouseHistory::getSeller)
                    .collect(Collectors.toSet());

            List<HousePortretPiece> portrets = new ArrayList<>();
            for (HouseHistory houseHistory : history) {
                HousePortretPiece housePortretPiece = new HousePortretPiece();
                Person person = portretService.getPerson(houseHistory.getBuyer());
                housePortretPiece.copy(person);

                housePortretPiece.setBuy(houseHistory.getBuyer() != null && !sellerIins.contains(houseHistory.getBuyer()));
                housePortretPiece.setDate(houseHistory.getDate());
                portrets.add(housePortretPiece);
            }

            return HousePortret.builder()
                    .kd(last.getKd_fixed())
                    .rka(last.getRka())
                    .volume(last.getObshaya_ploshad())
                    .currentOwner(history.get(history.size() - 1).getBuyer())
                    .portrets(portrets)
                    .build();
        }
        return null;
    }

    public CarPortret getCar(FindCarRequest request) throws IOException {
        log.info("➡ Starting getCar() for VIN='{}', GRNZ='{}'", request.getVin(), request.getGrnz());

        if (request.getVin() == null && request.getGrnz() == null) {
            log.warn("❌ VIN и GRNZ не указаны — возвращаю пустой портрет");
            return CarPortret.builder()
                    .cars(List.of())
                    .portrets(List.of())
                    .build();
        }

        Map<String, CarPortretPiece> pieceMap = new LinkedHashMap<>();
        Map<String, CarInfo> carInfoMap = new LinkedHashMap<>();

        // === 1️⃣ Владелец ===
        Car owner = carService.getOwner(request.getVin(), request.getGrnz());
        if (owner != null && owner.getIinOwner() != null) {
            Person ownerPerson = portretService.getPerson(owner.getIinOwner());
            if (ownerPerson != null) {
                CarPortretPiece ownerPiece = new CarPortretPiece();
                ownerPiece.copy(ownerPerson);
                ownerPiece.setRole("Владелец");
                ownerPiece.setStartDate(owner.getDateRegistration());
                pieceMap.put(ownerPerson.getIin(), ownerPiece);
                log.info("✅ Добавлен владелец: {} ({})", ownerPerson.getFio(), ownerPerson.getIin());
            }
        }

        // === 2️⃣ Страховка ===
        List<CarInsurance> insurances = carService.searchInsuranceCarByParams(request.getVin(), request.getGrnz());

        for (CarInsurance insurance : insurances) {
            // === Добавляем информацию о машине ===
            String carKey = insurance.getVin() != null ? insurance.getVin() : insurance.getGrnz();
            if (carKey != null && !carInfoMap.containsKey(carKey)) {
                CarInfo carInfo = new CarInfo();
                carInfo.setMark(insurance.getMarka());
                carInfo.setModel(insurance.getModel());
                carInfo.setVin(insurance.getVin());
                carInfo.setGrnz(insurance.getGrnz());
                carInfoMap.put(carKey, carInfo);
                log.info("🚗 Добавлена информация о машине: {} {} ({})", insurance.getMarka(), insurance.getModel(), insurance.getVin());
            }

            // === Страхователь ===
            if (insurance.getIinInsurer() != null) {
                String insurerIin = insurance.getIinInsurer();
                CarPortretPiece piece = pieceMap.get(insurerIin);

                if (piece == null) {
                    Person insurer = portretService.getPerson(insurerIin);
                    if (insurer != null) {
                        piece = new CarPortretPiece();
                        piece.copy(insurer);
                        piece.setRole("Страхователь");
                        pieceMap.put(insurerIin, piece);
                        log.info("✅ Добавлен страхователь: {} ({})", insurer.getFio(), insurer.getIin());
                    }
                }

                if (piece != null) {
                    piece.mergeDates(insurance.getStartDate(), insurance.getEndDate());
                }
            }

            // === Застрахованный ===
            if (insurance.getIinInsured() != null) {
                String insuredIin = insurance.getIinInsured();
                CarPortretPiece piece = pieceMap.get(insuredIin);

                if (piece == null) {
                    Person insured = portretService.getPerson(insuredIin);
                    if (insured != null) {
                        piece = new CarPortretPiece();
                        piece.copy(insured);
                        piece.setRole("Застрахованный");
                        pieceMap.put(insuredIin, piece);
                        log.info("✅ Добавлен застрахованный: {} ({})", insured.getFio(), insured.getIin());
                    }
                }

                if (piece != null) {
                    piece.mergeDates(insurance.getStartDate(), insurance.getEndDate());
                }
            }
        }

        // === 3️⃣ Результат ===
        List<CarPortretPiece> portrets = new ArrayList<>(pieceMap.values());
        List<CarInfo> cars = new ArrayList<>(carInfoMap.values());

        if (portrets.isEmpty() && cars.isEmpty()) {
            log.info("ℹ️ Для VIN={} GRNZ={} не найдено данных", request.getVin(), request.getGrnz());
        } else {
            log.info("✅ Собрано {} участников и {} автомобилей", portrets.size(), cars.size());
        }

        return CarPortret.builder()
                .cars(cars)
                .portrets(portrets)
                .build();
    }


}