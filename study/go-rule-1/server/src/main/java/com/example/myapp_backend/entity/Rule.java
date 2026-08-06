package com.example.myapp_backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Rule {
    private Long id;
    private String name;

    // raw JDM JSON as string
    private String decision;
}
