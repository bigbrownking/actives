package org.info.infobaza.service.portret;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.person.Age;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.dto.response.info.IinInfo;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.job.CompanyRecord;
import org.info.infobaza.model.info.person.DossierPerson;
import org.info.infobaza.model.info.person.nominal.Nominal;
import org.info.infobaza.model.info.person.nominal.NominalFiz;
import org.info.infobaza.model.info.person.nominal.NominalUl;
import org.info.infobaza.model.info.person.PersonRecord;
import org.info.infobaza.security.jwt.JwtTokenUtil;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.util.convert.NumberConverter;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortretService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;
    private final RestTemplate restTemplate;
    private final JwtTokenUtil jwtTokenUtil;
    private final NumberConverter numberConverter;

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String IIN_PATTERN = "\\d{12}";

    @Data
    private static class FetchResult {
        private String fullName;
        private Age age;
        private String photo;
        private String type;
    }

    public Person getPerson(String iin) throws IOException {
        validateIin(iin);
        log.info("Fetching person portrait for IIN: {}", iin);

        FetchResult fetchResult = fetchEntityData(iin);
        String fullName = fetchResult.getFullName() != null ? fetchResult.getFullName() : "Не найдено в базе данных";
        Age age = fetchResult.getAge();
        String photo = fetchResult.getPhoto();

        // Fetch portret data for status
        List<String> portrets = fetchPortrets(iin);
        List<String> status = determineStatus(age, portrets);

        return new Person(
                fullName,
                age != null ? age.getAge() : 0,
                iin,
                photo,
                status,
                isNominal(iin)
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

        // Step 1: Try dossier person
        try {
            DossierPerson dossierPerson = constructDossierPerson(iin);
            String fullName = String.format("%s %s %s",
                    dossierPerson.getLastName() != null ? dossierPerson.getLastName() : "",
                    dossierPerson.getFirstName() != null ? dossierPerson.getFirstName() : "",
                    dossierPerson.getPatronymic() != null ? dossierPerson.getPatronymic() : "").trim();
            if (!fullName.isEmpty()) {
                result.setFullName(fullName);
                result.setAge(dossierPerson.getAge());
                result.setPhoto(dossierPerson.getPhoto());
                result.setType("PERSON");
                log.info("Fetched person details from dossier for IIN: {}", iin);
                return result;
            }
        } catch (NotFoundException e) {
            log.debug("No person found in dossier for IIN: {}", iin);
        } catch (Exception e) {
            log.error("Error fetching dossier person for IIN: {}", iin, e);
        }

        // Step 2: Try fetchFIO_FL and fetchAge
        String fio = fetchFIO_FL(iin);
        if (fio != null && !fio.trim().isEmpty()) {
            result.setFullName(fio);
            result.setAge(fetchAge(iin));
            result.setType("PERSON");
            log.info("Fetched person FIO '{}' from fetchFIO_FL for IIN: {}", fio, iin);
            return result;
        }

        // Step 3: Try dossier UL
        try {
            String jsonResponse = getDossierULContent(iin);
            JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);
            JsonNode companyNode = rootNode.get(0);
            if (companyNode != null) {
                String companyName = companyNode.path("fullName").asText(null);
                if (companyName != null && !companyName.trim().isEmpty()) {
                    result.setFullName(companyName.trim());
                    result.setType("COMPANY");
                    log.info("Fetched company name '{}' from dossier UL for IIN: {}", companyName, iin);
                    return result;
                }
                log.debug("Company found but fullName is empty or null for IIN: {}", iin);
            } else {
                log.debug("No company record found in JSON array for IIN: {}", iin);
            }
        } catch (Exception e) {
            log.error("Error fetching company dossier for IIN: {}", iin, e);
        }

        // Step 4: Try fetchFIO_UL
        String companyName = fetchFIO_UL(iin);
        if (companyName != null && !companyName.trim().isEmpty()) {
            result.setFullName(companyName);
            result.setType("COMPANY");
            log.info("Fetched company name '{}' from fetchFIO_UL for IIN: {}", companyName, iin);
            return result;
        }

        return result;
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

    private DossierPerson constructDossierPerson(String iin) throws JsonProcessingException, NotFoundException {
        String jsonResponse = getDossierFLContent(iin);
        JsonNode rootNode = new ObjectMapper().readTree(jsonResponse);

        JsonNode mvFlNode = rootNode.path("mvFlList").get(0);
        if (mvFlNode == null) {
            log.warn("No person record found in mvFlList for IIN: {}", iin);
            throw new NotFoundException("No person found with IIN " + iin);
        }

        String firstName = mvFlNode.path("first_name").asText(null);
        String lastName = mvFlNode.path("last_name").asText(null);
        String patronymic = mvFlNode.path("patronymic").asText(null);
        String birthDateStr = mvFlNode.path("birth_date").asText(null);
        String deathDateStr = mvFlNode.path("death_date").asText(null);

        Age age = null;
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                LocalDate birthDate = LocalDate.parse(birthDateStr, formatter);
                LocalDate deathDate = deathDateStr != null && !deathDateStr.isEmpty()
                        ? LocalDate.parse(deathDateStr, formatter) : null;
                age = numberConverter.getAgeByDates(new LocalDate[]{birthDate, deathDate});
            } catch (Exception e) {
                log.error("Failed to parse birth/death dates for IIN: {}. Error: {}", iin, e.getMessage());
            }
        }

        String photo = null;
        JsonNode photoDbNode = rootNode.path("photoDbs").get(0);
        if (photoDbNode != null) {
            photo = photoDbNode.path("photo").asText(null);
            if (photo != null && photo.isEmpty()) {
                photo = null;
                log.warn("Photo field is empty for IIN: {}", iin);
            } else {
                log.info("Extracted photo for IIN: {}", iin);
            }
        }

        return new DossierPerson(firstName, lastName, patronymic, birthDateStr, deathDateStr, age, photo);
    }

    private String getDossierFLContent(String iin) {
        validateIin(iin);
        return fetchDossierContent(iin, "https://192.168.122.47/api/api/pandora/dossier/get-fl-by-iin", "IIN");
    }

    private String getDossierULContent(String bin) {
        validateIin(bin);
        return fetchDossierContent(bin, "https://192.168.122.47/api/api/pandora/dossier/ul/find-by-bin", "BIN");
    }

    private String fetchDossierContent(String identifier, String dossierUrl, String identifierType) {
        String dossierToken = jwtTokenUtil.generateDossierJwtToken();
        log.info("Generated token for {}: {}", identifierType, dossierToken);
        if (dossierToken == null || dossierToken.isEmpty()) {
            log.error("Access token not generated for {}", identifierType);
            throw new IllegalStateException("Access token is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + dossierToken);

        String url = dossierUrl + "?" + identifierType.toLowerCase() + "=" + identifier;
        log.info("Sending GET request to: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully fetched dossier content for {}: {}", identifierType, identifier);
                return response.getBody();
            }
            log.error("Failed to fetch dossier for {}: {}. Status: {}", identifierType, identifier, response.getStatusCode());
            throw new RuntimeException("Failed to fetch dossier: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Exception while fetching dossier for {}: {}. Error: {}", identifierType, identifier, e.getMessage());
            throw new RuntimeException("Failed to fetch dossier content", e);
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

    public Age fetchAge(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch FIO");
            return null;
        }

        String sql = "SELECT BIRTH_DATE, DEATH_DATE FROM gbd_fl0205.person_info WHERE IIN = ?";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

            LocalDate[] userDates = jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> {
                        String birthDateStr = rs.getString("BIRTH_DATE");
                        String deathDateStr = rs.getString("DEATH_DATE");
                        LocalDate birthDate = !birthDateStr.isEmpty() ? LocalDate.parse(birthDateStr, formatter) : null;
                        LocalDate deathDate = !deathDateStr.isEmpty() ? LocalDate.parse(deathDateStr, formatter) : null;
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
}