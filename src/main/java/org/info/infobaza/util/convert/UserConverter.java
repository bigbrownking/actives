package org.info.infobaza.util.convert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.main.Role;
import org.info.infobaza.model.main.User;
import org.info.infobaza.model.ser.AccessSer;
import org.info.infobaza.model.ser.UserSer;
import org.info.infobaza.repository.main.RoleRepository;
import org.info.infobaza.repository.main.UserRepository;
import org.info.infobaza.repository.ser.AccessSerRepository;
import org.info.infobaza.repository.ser.UserSerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserConverter {
    private final UserRepository userRepository;
    private final AccessSerRepository accessSerRepository;
    private final UserSerRepository userSerRepository;
    private final RoleRepository roleRepository;
    private final String email = "@afm.gov.kz";
    private final String emailPrefix = "ser_";

    public User fromSerToActive(UserSer userSer) {
        Role role = roleRepository.findByName("USER").orElseThrow(() -> new IllegalStateException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        return User.builder()
                .username(userSer.getIin())
                .email(emailPrefix + userSer.getIin() + email)
                .password(userSer.getPassword())
                .active(true)
                .updated_date(userSer.getUpdatedDate())
                .roles(roles)
                .build();
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void insertOrUpdateUsers() {
        List<UserSer> serUsers = userSerRepository.findAll();
        List<User> usersToSave = new ArrayList<>();

        for (UserSer userSer : serUsers) {
            try {
                if (isInvalidUser(userSer)) {
                    continue;
                }

                User converted = fromSerToActive(userSer);
                Optional<User> existing = userRepository.findByUsername(converted.getUsername());

                if (existing.isPresent()) {
                    User existingUser = existing.get();
                    // Check if update is necessary
                    if (needsUpdate(existingUser, converted, userSer)) {
                        log.info("Updating user: " + existingUser.getUsername());
                        existingUser.setPassword(converted.getPassword());
                        existingUser.setActive(converted.isActive());
                        existingUser.setEmail(converted.getEmail());
                        existingUser.setUpdated_date(converted.getUpdated_date());
                        usersToSave.add(existingUser);
                    }
                } else {
                    log.info("Inserting new user: " + converted.getUsername());
                    usersToSave.add(converted);
                }
            } catch (Exception e) {
                log.error("Failed to convert user: " + userSer.getIin(), e);
            }
        }

        if (!usersToSave.isEmpty()) {
            log.info("Saving {} users...", usersToSave.size());
            userRepository.saveAll(usersToSave);
            log.info("Successfully saved {} users", usersToSave.size());
        } else {
            log.info("No users to save or update");
        }
    }

    private boolean isInvalidUser(UserSer userSer) {
        if (userSer.getAccess() == null) {
            return true;
        }
       // boolean isValidStatus = userSer.getAccess().equals("FIRST_CATEGORY") || userSer.getAccess().equals("SECOND_CATEGORY");
        AccessSer access = accessSerRepository.findById(userSer.getId()).orElse(null);
        boolean isValidStatus = access != null && access.isHasAccess();
        return !isValidStatus ||
                (userSer.getPasswordExpDate().isBefore(LocalDateTime.now()) && userSer.getStatus().equals("DISABLED"));
    }

    private boolean needsUpdate(User existingUser, User convertedUser, UserSer userSer) {
        return !Objects.equals(existingUser.getPassword(), convertedUser.getPassword()) ||
                !Objects.equals(existingUser.isActive(), convertedUser.isActive()) ||
                !Objects.equals(existingUser.getEmail(), convertedUser.getEmail()) ||
                !Objects.equals(existingUser.getUpdated_date(), userSer.getUpdatedDate());
    }
}