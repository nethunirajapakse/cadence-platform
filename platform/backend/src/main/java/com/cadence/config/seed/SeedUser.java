package com.cadence.config.seed;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeedUser {
    private String name;
    private String email;
    private String password;
    private String role; // "TEAM_MEMBER" or "MANAGER" - matches RoleName exactly
}
