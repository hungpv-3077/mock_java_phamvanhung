package sun.asterisk.booking_tour.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import io.github.cdimascio.dotenv.Dotenv;

@Component
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> dotenvMap = new HashMap<>();

        dotenv.entries().forEach(entry -> {
            dotenvMap.put(entry.getKey(), entry.getValue());
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        environment.getPropertySources()
                .addFirst(new MapPropertySource("dotenvProperties", dotenvMap));
    }
}
