package org.unibl.etf.eosiguranje.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}