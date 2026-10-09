package com.mmaug.claimsagent.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByPolicyId(UUID policyId);

    List<Vehicle> findAllByPolicyIdAndRegistrationNumberIgnoreCase(UUID policyId, String registrationNumber);
}