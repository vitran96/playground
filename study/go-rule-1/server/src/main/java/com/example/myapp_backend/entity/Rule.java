package com.example.myapp_backend.entity;

import lombok.Data;

@Data
public class Rule {

    private Long id;
    private String name;
    private String decision; // raw JDM JSON as string
}
