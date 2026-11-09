package iuh.fit.demoprj.tracking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TrackingProperties.class)
public class TrackingConfiguration {
}
