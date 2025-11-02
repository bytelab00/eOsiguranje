package org.unibl.etf.eosiguranje.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.service.PolicyService;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public List<Policy> getAll() {
        return policyService.findAll();
    }

    @GetMapping("/{id}")
    public Policy getById(@PathVariable Long id) {
        return policyService.findById(id).orElse(null);
    }
}
