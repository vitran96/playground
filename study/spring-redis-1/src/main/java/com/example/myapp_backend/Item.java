package com.example.myapp_backend;

import java.io.Serializable;

public record Item(Long id, String name, String category, double price) implements Serializable {
}
