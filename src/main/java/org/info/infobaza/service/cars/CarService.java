package org.info.infobaza.service.cars;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.object.Car;
import org.info.infobaza.model.info.object.CarInsurance;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CarService {
    private final SQLFileUtil sqlFileUtil;
    private final JdbcTemplate jdbcTemplate;
    private final Mapper mapper;

    public List<CarInsurance> searchInsuranceCarByParams(String vin, String grnz) throws IOException {
        String sql;
        List<CarInsurance> cars;

        if (vin != null && !vin.isBlank()) {
            sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.CARS_insurance_vin.getPath(), vin);
            cars = jdbcTemplate.query(sql, mapper::mapRowToCarInsurance);
        } else if (grnz != null && !grnz.isBlank()) {
            sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.CARS_insurance_grnz.getPath(), grnz);
            cars = jdbcTemplate.query(sql, mapper::mapRowToCarInsurance);
        } else {
            return List.of();
        }
        log.info("SQL: {}", sql);

        return cars.stream().distinct().toList();
    }

    public Car getOwner(String vin, String grnz) throws IOException {
        String sql = "";
        List<Car> cars = new ArrayList<>();
        if (vin != null) {
            sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.CARS_owner_vin.getPath(), vin);
            cars = jdbcTemplate.query(sql, mapper::mapRowToCar);
            cars = cars.stream().filter(x -> x.getVin().equals(vin)).distinct().toList();
        } else if (grnz != null) {
            sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.CARS_owner_grnz.getPath(), grnz);
            cars = jdbcTemplate.query(sql, mapper::mapRowToCar);
            cars = cars.stream().filter(x -> x.getGrnz().equals(grnz)).distinct().toList();
        }
        if(cars.isEmpty()){
            return null;
        }
        Car car = cars.get(0);
        car.setModel(car.getMark().split(" ")[1]);
        car.setMark(car.getMark().split(" ")[0]);
        log.info("CARS {}", cars);
        log.info("SQL {}", sql);
        return cars.get(0);
    }

}
