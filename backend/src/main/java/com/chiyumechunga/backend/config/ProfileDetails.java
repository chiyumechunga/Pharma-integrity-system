package com.chiyumechunga.backend.config;

import com.chiyumechunga.backend.model.SupplyChainParticipant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class ProfileDetails implements UserDetails {

    private final SupplyChainParticipant participant;

    public ProfileDetails(SupplyChainParticipant participant) {
        this.participant = participant;
    }

    // --- 1. Mapping: Entity Role -> Security Authority ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converts Enum (e.g., MANUFACTURER) to String authority
        return List.of(new SimpleGrantedAuthority(participant.getRole().name()));
    }

    // --- 2. Mapping: Entity Credentials ---
    @Override
    public String getPassword() {
        return participant.getPassword(); // Returns the hash from DB
    }

    @Override
    public String getUsername() {
        return participant.getEmail(); // Uses Email as the unique username
    }

    // --- 3. Standard Boilerplate (Always True for this system) ---
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return participant.isActive(); // Links to 'is_active' column
    }

    // --- 4. Helper: Access the raw data later in Controllers ---
    public SupplyChainParticipant getProfile() {
        return participant;
    }
}