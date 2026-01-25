package sun.asterisk.booking_tour.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import sun.asterisk.booking_tour.entity.Role;
import sun.asterisk.booking_tour.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Slf4j
public class DataSeeder {
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    @Bean
    public CommandLineRunner seedData(RoleRepository roleRepository) {
        return args -> {
            // Seed roles if not exist
            if (!roleRepository.existsByName("ADMIN")) {
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                roleRepository.save(adminRole);
                logger.info("Created ADMIN role");
            }

            if (!roleRepository.existsByName("USER")) {
                Role userRole = new Role();
                userRole.setName("USER");
                roleRepository.save(userRole);
                logger.info("Created USER role");
            }

            logger.info("Data seeding completed. Total roles: {}", roleRepository.count());
        };
    }
}
