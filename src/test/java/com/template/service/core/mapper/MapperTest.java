package com.template.service.core.mapper;

class MapperTest {

    private Mapper mapper;

//    @BeforeEach
//    void setUp() {
//        ModelMapper modelMapper = new ModelMapper();
//        modelMapper.getConfiguration()
//                .setSkipNullEnabled(true)
//                .setAmbiguityIgnored(true)
//                .setPropertyCondition(Conditions.isNotNull());
//        mapper = new Mapper(modelMapper, new NestedPatchResolver());
//    }
//
//    @Test
//    void mapToPatchUpdatesMatchingNestedItemsAndSkipsNulls() {
//        Customer existing = existingCustomer();
//        Customer dto = Customer.builder()
//                .id("12334")
//                .names("Victor A. Updated")
//                .addresses(new HashSet<>(Set.of(
//                        Address.builder().id("12345").address("Calle Verdadera 456").build(),
//                        Address.builder().id("99999").address("Nueva 789").city("Quito").build()
//                )))
//                .paymentTerm(PaymentTerm.builder().id(1).name("Termino actualizado").build())
//                .build();
//
//        Customer patched = mapper.mapToPatch(dto, existing, "id",
//                NestedPatchDef.of("addresses", "id", Address.class),
//                NestedPatchDef.of("paymentTerm", "id", PaymentTerm.class));
//
//        assertThat(patched.getNames()).isEqualTo("Victor A. Updated");
//        assertThat(patched.getOdooId()).isEqualTo("12345-Odoo");
//        assertThat(patched.getCreditLimit()).isEqualTo(2999.1);
//        assertThat(patched.getPaymentTerm().getName()).isEqualTo("Termino actualizado");
//        assertThat(patched.getAddresses()).hasSize(2);
//        assertThat(addressById(patched, "12345").getAddress()).isEqualTo("Calle Verdadera 456");
//        assertThat(addressById(patched, "12345").getProvince()).isEqualTo("Buenos Aires");
//    }
//
//    @Test
//    void mapToPatchSkipsWhenCheckFieldDiffers() {
//        Customer existing = existingCustomer();
//        Customer dto = Customer.builder().id("OTHER").names("Should Not Apply").build();
//
//        Customer patched = mapper.mapToPatch(dto, existing, "id");
//
//        assertThat(patched.getNames()).isEqualTo("Victor Arreaga");
//    }
//
//    @Test
//    void mapToCreateCopiesProvidedFields() {
//        Customer dto = Customer.builder().id("1").names("New Customer").creditLimit(100.0).build();
//
//        Customer created = mapper.mapToCreate(dto, new Customer());
//
//        assertThat(created.getId()).isEqualTo("1");
//        assertThat(created.getNames()).isEqualTo("New Customer");
//        assertThat(created.getCreditLimit()).isEqualTo(100.0);
//    }
//
//    private Customer existingCustomer() {
//        return Customer.builder()
//                .id("12334")
//                .odooId("12345-Odoo")
//                .names("Victor Arreaga")
//                .creditLimit(2999.1)
//                .addresses(new HashSet<>(Set.of(
//                        Address.builder().id("12345").address("Calle Falsa 123")
//                                .province("Buenos Aires").city("Springfield").build()
//                )))
//                .paymentTerm(PaymentTerm.builder().id(1).name("Prueba de termino").build())
//                .build();
//    }
//
//    private Address addressById(Customer customer, String id) {
//        return customer.getAddresses().stream()
//                .filter(address -> id.equals(address.getId()))
//                .findFirst()
//                .orElseThrow();
//    }
}
