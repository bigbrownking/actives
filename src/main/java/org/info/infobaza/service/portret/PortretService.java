package org.info.infobaza.service.portret;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.dto.response.person.Age;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.dto.response.info.IinInfo;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.model.info.job.CompanyRecord;
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

    public Person getPerson(String iin) throws IOException, NotFoundException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching person portret for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Портрет_Общий.getPath(), iin);
        try {
            List<PersonRecord> persons = jdbcTemplate.query(sql, mapper::mapRowToPortret);

            List<Nominal> nominals;
            String nominalFizSql = "select * from pfr_dashboard.nominals_1 where iin_bin = ?";
            nominals = jdbcTemplate.query(
                    nominalFizSql,
                    new Object[]{iin}, mapper::mapRowToNominalFiz
            );

            if(nominals.isEmpty()){
                String nominalUlSql = "select * from pfr_dashboard.nominal_ul where iin_bin = ?";
                nominals = jdbcTemplate.query(
                        nominalUlSql,
                        new Object[]{iin}, mapper::mapRowToNominalUl
                );
            }

            List<String> portrets = persons.stream().map(PersonRecord::getPortret).distinct().toList();
            Age age = fetchAge(iin);

            List<String> status;
            if(age == null){
                status = Collections.singletonList("Нет сведений");
            }
            else if(!age.isStatus()){
                status = Collections.singletonList("Умерший");
            }
            else{
               status = !portrets.isEmpty() ? portrets : null;
            }
            return new Person(getIinInfo(
                    iin).getName(),
                    age == null ? 0 : age.getAge(),
                    iin,
                    extractPhoto(iin),
                    status,
                    !nominals.isEmpty());
        } catch (EmptyResultDataAccessException e) {
            log.warn("No person record found for IIN: {}", iin);
            throw new NotFoundException("No person found with iin " + iin);
        }
    }

    private String getDossierContent(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }

        String dossierToken = jwtTokenUtil.generateDossierJwtToken();

        if (dossierToken == null || dossierToken.isEmpty()) {
            log.error("Access token not generated");
            throw new IllegalStateException("Access token is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + dossierToken);

        String dossierUrl = "https://192.168.122.47/api/api/pandora/dossier/iin";
        String url = dossierUrl + "?iin=" + iin;
        log.info("Sending GET request to: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully fetched dossier content for IIN: {}", iin);
                return response.getBody();
            } else {
                log.error("Failed to fetch dossier for IIN: {}. Status: {}", iin, response.getStatusCode());
                throw new RuntimeException("Failed to fetch dossier: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Exception while fetching dossier for IIN: {}. Error: {}", iin, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch dossier content", e);
        }
    }

    private String extractPhoto(String iin) {
        try {
            String jsonResponse = getDossierContent(iin);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode contentNode = rootNode.path("content").get(0);
            String photo = contentNode.path("photo").asText();
            if (photo.isEmpty()) {
                log.warn("Photo field is empty for IIN: {}", iin);
                return null;
            }
            log.info("Extracted photo for IIN: {}", iin);
            return photo;
        } catch (Exception e) {
            log.error("Failed to extract photo for IIN: {}. Error: {}", iin, e.getMessage());
            return null;
        }
    }
    public String fetchFIO_FL(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch FIO");
            return null;
        }

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

    public IinInfo getIinInfo(String iin) {
        if (iin == null || iin.trim().isEmpty()) {
            log.warn("IIN is null or empty, cannot fetch info");
            return null;
        }

        log.info("Checking IIN type for: {}", iin);

        String fio = fetchFIO_FL(iin);
        if (fio != null && !fio.isEmpty()) {
            log.info("IIN {} is a person with FIO: {}", iin, fio);
            return new IinInfo("PERSON", fio, iin);
        }
        // Try company
        String companySql = "SELECT * FROM gdb_ul0205.ztFaces WHERE `BIN subekta` = ?";
        try {
            List<CompanyRecord> companyRecords = jdbcTemplate.query(
                    companySql,
                    new Object[]{iin}, mapper::mapRowToCompany
            );
            Optional<CompanyRecord> newestCompany = companyRecords.stream()
                    .filter(record -> record.getDateReg() != null)
                    .max(Comparator.comparing(CompanyRecord::getDateReg));
            if (newestCompany.isPresent()) {
                CompanyRecord record = newestCompany.get();
                String rusName = record.getRusName() != null ? record.getRusName() : "";
                String origName = record.getOrigName() != null ? record.getOrigName() : "";
                String companyName = (rusName + " " + origName).trim();
                if (!companyName.isEmpty()) {
                    log.info("IIN {} is a company with name: {}", iin, companyName);
                    return new IinInfo("COMPANY", companyName, iin);
                } else {
                    log.debug("Company found but name is empty for IIN: {}", iin);
                }
            } else {
                log.debug("No valid company record found for IIN: {}", iin);
            }
        } catch (EmptyResultDataAccessException e) {
            log.debug("No company found for IIN: {}", iin);
        } catch (Exception e) {
            log.error("Error fetching company info for IIN: {}", iin, e);
        }
        log.warn("No person or company found for IIN: {}", iin);
        return null;
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
}
