package org.unibl.etf.eosiguranje.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.unibl.etf.eosiguranje.model.Policy;
import org.unibl.etf.eosiguranje.service.PolicyService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/policies")
@RequiredArgsConstructor
public class AdminPolicyController {

    private final PolicyService policyService;

    @GetMapping
    public List<Policy> getAll() {
        return policyService.findAll();
    }

    @PostMapping
    public Policy create(@RequestBody Policy policy) {
        return policyService.save(policy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Policy> update(@PathVariable Long id, @RequestBody Policy policy) {
        return policyService.findById(id)
                .map(existing -> {
                    existing.setName(policy.getName());
                    existing.setType(policy.getType());
                    existing.setDescription(policy.getDescription());
                    existing.setPrice(policy.getPrice());
                    return ResponseEntity.ok(policyService.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        policyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
