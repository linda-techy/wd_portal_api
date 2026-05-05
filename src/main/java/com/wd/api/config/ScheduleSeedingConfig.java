package com.wd.api.config;

/**
 * Reserved package config for scheduling seeders.
 *
 * <p>Note: we deliberately do NOT expose a generic Jackson YAML
 * {@code ObjectMapper} bean here — registering one as type
 * {@code ObjectMapper} pollutes Spring's HTTP message-converter chain
 * (it prefers the most-recently-registered ObjectMapper for content
 * negotiation), causing controllers to render YAML for JSON requests.
 *
 * <p>Each seeder ({@code HolidaySeeder}, {@code WbsTemplateSeeder})
 * therefore instantiates its own private YAML mapper. The cost is one
 * extra mapper instance at boot; the benefit is HTTP responses stay
 * untouched.
 */
public final class ScheduleSeedingConfig {
    private ScheduleSeedingConfig() {}
}
