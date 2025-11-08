package org.unibl.etf.eosiguranje.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.unibl.etf.eosiguranje.model.SecurityEvent;
import org.unibl.etf.eosiguranje.repository.SecurityEventRepository;
import org.unibl.etf.eosiguranje.service.PolicyService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class AccessControlFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final SecurityEventRepository eventRepository;
    private final PolicyService policyService;
    private final SecurityEventRepository securityEventRepository;

    public AccessControlFilter(ObjectMapper objectMapper,
                               SecurityEventRepository eventRepository,
                               PolicyService policyService, SecurityEventRepository securityEventRepository) {
        this.objectMapper = objectMapper;
        this.eventRepository = eventRepository;
        this.policyService = policyService;
        this.securityEventRepository = securityEventRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/purchase") &&
                request.getMethod().equalsIgnoreCase("POST")) {
            try {
                String body = request.getReader().lines().reduce("", (acc, line) -> acc + line);
                if (body.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                JsonNode root = objectMapper.readTree(body);
                JsonNode priceNode = root.get("price");
                JsonNode policyIdNode = root.get("policyId");

                if (priceNode == null || policyIdNode == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                double sentPrice = priceNode.asDouble();
                long policyId = policyIdNode.asLong();
                BigDecimal expectedPrice = policyService.getExpectedPrice(policyId);
                double expectedPriceDouble = expectedPrice.doubleValue();
                // allow deviation up to ±5%
                double lowerBound = expectedPriceDouble * 0.95;
                double upperBound = expectedPriceDouble * 1.05;

                if (sentPrice < lowerBound || sentPrice > upperBound) {
                    SecurityEvent event = SecurityEvent.builder()
                            .action("PAYMENT_ANOMALY")
                            .details("Price manipulation detected for policyId=" + policyId)
                            .riskScore(90)
                            .createdAt(LocalDateTime.now())
                            .build();

                    securityEventRepository.save(event);

                    eventRepository.save(event);

                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Price manipulation detected");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace(); // avoid 500 crash
            }
        }

        filterChain.doFilter(request, response);
    }
}
