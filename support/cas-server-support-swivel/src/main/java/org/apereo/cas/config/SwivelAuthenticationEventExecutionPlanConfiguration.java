package org.apereo.cas.config;

import org.apereo.cas.adaptors.swivel.SwivelAuthenticationHandler;
import org.apereo.cas.adaptors.swivel.SwivelMultifactorAuthenticationProvider;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.metadata.AuthenticationContextAttributeMetaDataPopulator;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProperties;
import org.apereo.cas.services.DefaultMultifactorAuthenticationProviderBypass;
import org.apereo.cas.services.MultifactorAuthenticationProvider;
import org.apereo.cas.services.MultifactorAuthenticationProviderBypass;
import org.apereo.cas.services.ServicesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is {@link SwivelAuthenticationEventExecutionPlanConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Configuration("swivelAuthenticationEventExecutionPlanConfiguration")
public class SwivelAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {
    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    @Bean
    @RefreshScope
    public AuthenticationMetaDataPopulator swivelAuthenticationMetaDataPopulator() {
        final String authenticationContextAttribute = casProperties.getAuthn().getMfa().getAuthenticationContextAttribute();
        return new AuthenticationContextAttributeMetaDataPopulator(authenticationContextAttribute,
                swivelAuthenticationHandler(), swivelAuthenticationProvider());
    }

    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypass swivelBypassEvaluator() {
        return new DefaultMultifactorAuthenticationProviderBypass(casProperties.getAuthn().getMfa().getSwivel().getBypass());
    }

    @ConditionalOnMissingBean(name = "swivelPrincipalFactory")
    @Bean
    public PrincipalFactory swivelPrincipalFactory() {
        return new DefaultPrincipalFactory();
    }

    @Bean
    @RefreshScope
    public SwivelAuthenticationHandler swivelAuthenticationHandler() {
        final MultifactorAuthenticationProperties.Swivel swivel = this.casProperties.getAuthn().getMfa().getSwivel();
        return new SwivelAuthenticationHandler(swivel.getName(),
                servicesManager, swivelPrincipalFactory(), swivel);
    }

    @Bean
    @RefreshScope
    public MultifactorAuthenticationProvider swivelAuthenticationProvider() {
        final SwivelMultifactorAuthenticationProvider p =
                new SwivelMultifactorAuthenticationProvider(swivelAuthenticationHandler());
        p.setBypassEvaluator(swivelBypassEvaluator());
        p.setGlobalFailureMode(casProperties.getAuthn().getMfa().getGlobalFailureMode());
        p.setOrder(casProperties.getAuthn().getMfa().getSwivel().getRank());
        p.setId(casProperties.getAuthn().getMfa().getSwivel().getId());
        return p;
    }

    @Override
    public void configureAuthenticationExecutionPlan(final AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(swivelAuthenticationHandler());
        plan.registerMetadataPopulator(swivelAuthenticationMetaDataPopulator());
    }

}
