package org.info.infobaza.service.portret;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.info.IinInfo;
import org.info.infobaza.dto.response.person.Age;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.model.dossierprime.MvFl;
import org.info.infobaza.model.dossierprime.MvFlWithPhotoDto;
import org.info.infobaza.model.dossierprime.PhotoDb;
import org.info.infobaza.model.dossierprime.ULDto;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.job.SupervisorRecord;
import org.info.infobaza.model.info.person.PersonRecord;
import org.info.infobaza.model.info.person.nominal.Nominal;
import org.info.infobaza.model.info.person.nominal.NominalRucUchr;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.service.DossierService;
import org.info.infobaza.service.enpf.HeadService;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.NumberConverter;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.info.infobaza.util.date.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.info.infobaza.util.convert.IinChecker.isUl;


@Service
@Slf4j
@RequiredArgsConstructor
public class PortretService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final DateUtil dateUtil;
    private final Mapper mapper;
    private HeadService headService;
    private final DossierService dossierService;
    private final NumberConverter numberConverter;
    private Analyzer analyzer;
    private static final String IIN_PATTERN = "\\d{12}";
    @Autowired
    public void setHeadService(@Lazy HeadService headService) {
        this.headService = headService;
    }

    @Autowired
    public void setAnalyzer(@Lazy Analyzer analyzer){ this.analyzer = analyzer;}
    @Data
    private static class FetchResult {
        private String fullName;
        private Age age;
        private String photo;
        private String type;
    }
    public Person getPersonWithDates(String iin, String dateFrom, String dateTo) throws IOException {
        Double totalActives = analyzer.calculateTotalActivesForPerson(iin, dateFrom, dateTo);
        Double totalIncomes = analyzer.calculateTotalIncomeByIIN(iin, dateFrom, dateTo);
        Person person = getPerson(iin);

        person.setActives(numberConverter.formatNumber(totalActives));
        person.setIncomes(numberConverter.formatNumber(totalIncomes));

        return person;
    }

    public Person getPerson(String iin) throws IOException {
        validateIin(iin);
        log.info("🧠 Fetching person portrait for IIN: {}", iin);

        FetchResult fetchResult = fetchEntityData(iin);

        String fullName = fetchResult.getFullName() != null
                ? fetchResult.getFullName()
                : "Не найдено в базе данных";
        Age age = fetchResult.getAge();
        String photo = fetchResult.getPhoto();

        List<String> portrets = fetchPortrets(iin);
        List<String> status = determineStatus(age, portrets);
        boolean turnover = fetchTurnover(iin);
        boolean nominal = isNominal(iin);
        boolean nominalUl = isNominalUl(iin);
        return new Person(
                fullName,
                age != null ? age.getAge() : 0,
                iin,
                photo,
                status,
                turnover,
                "0", "0",
                nominal,
                nominalUl
        );
    }


    public IinInfo getIinInfo(String iin) throws IOException {
        validateIin(iin);
        log.info("Checking IIN type for: {}", iin);

        FetchResult fetchResult = fetchEntityData(iin);
        if (fetchResult.getFullName() != null && !fetchResult.getFullName().trim().isEmpty()) {
            log.info("IIN {} is a {} with name: {}", iin, fetchResult.getType(), fetchResult.getFullName());
            return new IinInfo(fetchResult.getType(), fetchResult.getFullName(), iin);
        }

        log.warn("No person or company found for IIN: {}", iin);
        return null;
    }

    private FetchResult fetchEntityData(String iin) throws IOException {
        FetchResult result = new FetchResult();
        if(isUl(iin)){
            try {
                List<ULDto> ulList = dossierService.findUlByBin(iin);
                if (ulList != null && !ulList.isEmpty()) {
                    ULDto ul = ulList.get(0);
                    if (ul.getFullName() != null && !ul.getFullName().trim().isEmpty()) {
                        result.setFullName(ul.getFullName().trim());
                        result.setType("COMPANY");
                        log.info("Fetched COMPANY from DossierService: {}", ul.getFullName());
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching UL from DossierService for BIN: {}", iin, e);
            }
            String companyName = fetchFIO_UL(iin);
            if (companyName != null && !companyName.trim().isEmpty()) {
                result.setFullName(companyName);
                result.setType("COMPANY");
                log.info("Fetched company name '{}' from fetchFIO_UL for IIN: {}", companyName, iin);
                return result;
            }

            String companyNameRucUchr = fetchFIO_UL_RUC_UHCR(iin);
            if(companyNameRucUchr != null && !companyNameRucUchr.trim().isEmpty()){
                result.setFullName(companyNameRucUchr);
                result.setType("COMPANY");
                log.info("Fetched company name '{}' from fetchFIO_UL_RUC_UCHR for IIN: {}", companyName, iin);
                return result;
            }
            return result;
        }
        else {
            try {
                MvFlWithPhotoDto flDto = dossierService.getMvFl(iin);
                if (flDto != null && flDto.getMvFlList() != null && !flDto.getMvFlList().isEmpty()) {
                    MvFl fl = flDto.getMvFlList().get(0);

                    String fullName = String.format("%s %s %s",
                            s(fl.getLast_name()),
                            s(fl.getFirst_name()),
                            s(fl.getPatronymic())).trim();

                    if (!fullName.isBlank()) {
                        Age age = calculateAge(fl.getBirth_date(), fl.getDeath_date());
                        String photoBase64 = extractPhoto(flDto.getPhotoDbs());

                        result.setFullName(fullName);
                        result.setAge(age);
                        result.setPhoto(photoBase64);
                        result.setType("PERSON");
                        log.info("Fetched PERSON from DossierService: {}", fullName);
                        return result;
                    }
                }
            } catch (Exception e) {
                log.warn("Error fetching FL from DossierService for IIN: {}", iin, e);
            }
            String fio = fetchFIO_FL(iin);
            if (fio != null && !fio.trim().isEmpty()) {
                result.setFullName(fio);
                result.setAge(fetchAge(iin));
                result.setType("PERSON");
                log.info("Fetched person FIO '{}' from fetchFIO_FL for IIN: {}", fio, iin);
                return result;
            }
        }
        result.setType("UNKNOWN");
        result.setAge(null);
        result.setPhoto(null);
        result.setFullName("UNKNOWN");

        return result;
    }

    private String s(String str) {
        return str != null ? str.trim() : "";
    }
    private List<String> fetchPortrets(String iin) {
        try {
            String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Портрет_Общий.getPath(), iin);
            List<PersonRecord> persons = jdbcTemplate.query(sql, mapper::mapRowToPortret);
            return persons.stream().map(PersonRecord::getPortret).distinct().toList();
        } catch (EmptyResultDataAccessException e) {
            log.warn("No person record found for IIN: {}", iin);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching portret data for IIN: {}", iin, e);
            return Collections.emptyList();
        }
    }
    private Age calculateAge(String birthDateStr, String deathDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) return null;
        try {
            LocalDate birthDate = dateUtil.formatPortretDate(birthDateStr);
            LocalDate deathDate = deathDateStr != null ? dateUtil.formatPortretDate(deathDateStr) : null;
            return numberConverter.getAgeByDates(new LocalDate[]{birthDate, deathDate});
        } catch (Exception e) {
            log.error("Failed to calculate age for dates: {} / {}", birthDateStr, deathDateStr, e);
            return null;
        }
    }

    private String extractPhoto(List<PhotoDb> photoDbs) {
        if (photoDbs == null || photoDbs.isEmpty()) {
            return null;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        Optional<PhotoDb> latestPhoto = photoDbs.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getPhoto() != null && p.getPhoto().length > 0)
                .filter(p -> p.getDate() != null && !p.getDate().trim().isEmpty())
                .max(Comparator.comparing(
                        p -> {
                            try {
                                return LocalDate.parse(p.getDate().trim(), formatter);
                            } catch (DateTimeParseException e) {
                                return LocalDate.MIN;
                            }
                        }
                ));

        return latestPhoto.map(photoDb -> Base64.getEncoder().encodeToString(photoDb.getPhoto())).orElse(null);
    }


    private boolean fetchTurnover(String iin) {
        try {
            String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Turnover_turnoverFlag.getPath(), iin);
            List<String> turnovers = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("iin_bin"));
            return !turnovers.isEmpty();
        } catch (Exception e) {
            log.error("Error fetching turnoverBank.sql data for IIN: {}", iin, e);
            return false;
        }
    }


    private List<String> determineStatus(Age age, List<String> portrets) {
        if (age != null) {
            return !age.isStatus() ? Collections.singletonList("Умерший") :
                    !portrets.isEmpty() ? portrets : Collections.singletonList("Нет сведений");
        }
        return !portrets.isEmpty() ? portrets : Collections.singletonList("Нет сведений");
    }

    private void validateIin(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty");
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        if (!iin.matches(IIN_PATTERN)) {
            log.warn("Invalid IIN format: {}", iin);
            throw new IllegalArgumentException("IIN must be a 12-digit number");
        }
    }
    public String fetchFIO_FL(String iin) {
        validateIin(iin);
        String sql = "SELECT SURNAME, FIRSTNAME, SECONDNAME FROM gbd_fl0205.person_info WHERE IIN = ?";
        try {
            String fio = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> {
                        String surname = rs.getString("SURNAME");
                        String firstname = rs.getString("FIRSTNAME");
                        String secondname = rs.getString("SECONDNAME");
                        return String.format("%s %s %s", surname != null ? surname : "",
                                firstname != null ? firstname : "",
                                secondname != null ? secondname : "").trim();
                    },
                    iin
            );
            if (fio.isEmpty()) {
                log.warn("Empty FIO constructed for IIN: {}", iin);
                return null;
            }
            log.info("Fetched FIO '{}' for IIN: {}", fio, iin);
            return fio;
        } catch (EmptyResultDataAccessException e) {
            log.warn("No FIO found for IIN: {}", iin);
            return null;
        } catch (Exception e) {
            log.error("Error fetching FIO for IIN: {}", iin, e);
            return null;
        }
    }

    public String fetchFIO_UL(String bin) throws IOException {
        validateIin(bin);
        String companySql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.UL_zt.getPath(), bin);
        try {
            List<CompanyRecord> companyRecords = jdbcTemplate.query(companySql, mapper::mapRowToCompany);
            Optional<CompanyRecord> newestCompany = companyRecords.stream()
                    .filter(record -> record.getDateReg() != null)
                    .max(Comparator.comparing(CompanyRecord::getDateReg));

            if (newestCompany.isPresent()) {
                CompanyRecord record = newestCompany.get();
                String rusName = record.getRusName() != null ? record.getRusName().trim() : "";
                String origName = record.getOrigName() != null ? record.getOrigName().trim() : "";
                String companyName = (rusName + " " + origName).trim();

                if (!companyName.isEmpty()) {
                    log.info("Fetched company name '{}' for BIN: {}", companyName, bin);
                    return companyName;
                }
                log.debug("Company found but name is empty for BIN: {}", bin);
                return null;
            }
            log.debug("No valid company record found for BIN: {}", bin);
            return null;
        } catch (Exception e) {
            log.error("Error fetching company info for BIN: {}", bin, e);
            return null;
        }
    }

    public String fetchFIO_UL_RUC_UHCR(String bin) throws IOException {
        validateIin(bin);
        List<SupervisorRecord> supervisorRecords = headService.getUlType(bin);
        return supervisorRecords.stream().filter(x -> x.getTaxpayer_iin_bin().equals(bin)).map(SupervisorRecord::getTaxpayerName).findAny().orElse(null);
    }
    public Age fetchAge(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch FIO");
            return null;
        }

        String sql = "SELECT BIRTH_DATE, DEATH_DATE FROM gbd_fl0205.person_info WHERE IIN = ?";
        try {

            LocalDate[] userDates = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> {
                        String birthDateStr = rs.getString("BIRTH_DATE");
                        String deathDateStr = rs.getString("DEATH_DATE");
                        LocalDate birthDate = dateUtil.formatAgeDate(birthDateStr);
                        LocalDate deathDate = dateUtil.formatAgeDate(deathDateStr);
                        return new LocalDate[]{birthDate, deathDate};
                    },
                    iin
            );
            Age age = numberConverter.getAgeByDates(userDates);
            log.info("Fetched age '{}' for IIN: {}", age, iin);
            return age;
        } catch (EmptyResultDataAccessException e) {
            log.warn("No FIO found for IIN: {}", iin);
            return null;
        } catch (Exception e) {
            log.error("Error fetching FIO for IIN: {}", iin, e);
            return null;
        }
    }

    public boolean isNominal(String iin_bin) throws IOException {
        validateIin(iin_bin);
        String nominalFizSql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Nominal_fiz.getPath(), iin_bin);
        List<Nominal> nominals = jdbcTemplate.query(nominalFizSql, mapper::mapRowToNominalFiz);

        if (nominals.isEmpty()) {
            String nominalUlSql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Nominal_ul.getPath(), iin_bin);
            nominals = jdbcTemplate.query(nominalUlSql, mapper::mapRowToNominalUl);
        }
        return !nominals.isEmpty();
    }

    public boolean isNominalUl(String iin_bin) throws IOException {
        validateIin(iin_bin);
        String nominalUlSql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_is_nominal_ruc_uchr.getPath(), iin_bin);
        List<NominalRucUchr> nominalRucUchrs = jdbcTemplate.query(nominalUlSql, mapper::mapRowToNominalRucUch);
        if (nominalRucUchrs.isEmpty()) {
            String nominalUlSql1 = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Supervisor_is_nominal.getPath(), iin_bin);
            List<NominalRucUchr> nominalRucUchrs1 = jdbcTemplate.query(nominalUlSql1, mapper::mapRowToNominalRucUch);
            return !nominalRucUchrs1.isEmpty();
        }
        return true;
    }
}