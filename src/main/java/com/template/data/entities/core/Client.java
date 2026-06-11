package com.template.data.entities.core;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String phone;
}