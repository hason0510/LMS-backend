package com.example.backend.dto.response.permission;

public class PermissionInfoResponse {

    private Integer id;
    private String name;

    public PermissionInfoResponse() {}

    public PermissionInfoResponse(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
