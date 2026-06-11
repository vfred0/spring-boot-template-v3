package com.template.data.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.Set;

import static java.sql.Types.ARRAY;
import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Entity
@Table(name = "customers")
public class Customer {

    String odooId;

    @Id
    String id;

    String dni;

    @Column(nullable = false)
    String names;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "customer_categories",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"),
            foreignKey = @ForeignKey(name = "fk_customer_category_customer"),
            inverseForeignKey = @ForeignKey(name = "fk_customer_category_category")
    )
    Set<Category> categories;

    @JdbcTypeCode(ARRAY)
    Set<String> emails;

    String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_term_id")
    PaymentTerm paymentTerm;

    Double creditLimit;

    @Builder.Default
    Boolean isArchived = false;

    @Column(nullable = false)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "customer", cascade = CascadeType.ALL)
    Set<Address> addresses;

    Set<String> regionals;

    String channel;

    String segment;

    String commercial;

    String salesTeam;

    String province;

    String city;

}