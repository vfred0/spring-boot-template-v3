package com.template.data.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import com.template.api.dtos.customer.AddressDto;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    String id;

    @Column(columnDefinition = "TEXT")
    String address;

    @ManyToOne(fetch = LAZY)
    Customer customer;

    String province;

    String city;

    public Address patch(AddressDto addressDto) {
        if (addressDto.getAddress() != null) {
            this.address = addressDto.getAddress();
        }

        if (addressDto.getProvince() != null) {
            this.province = addressDto.getProvince();
        }

        if (addressDto.getCity() != null) {
            this.city = addressDto.getCity();
        }


        return this;
    }
}
