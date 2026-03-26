package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.Role;

public class RoleUpdateDTO {
    private Role role;

    public void setRole(Role role){
        this.role = role;
    }

    public Role getRole(){
        return this.role;
    }
}
