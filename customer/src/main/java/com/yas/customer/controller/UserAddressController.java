package com.yas.customer.controller;

import com.yas.customer.service.UserAddressService;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserAddressController {
    private static final String USER_ADDRESS_PATH = "/storefront/user-address";
    private static final String USER_ADDRESSES_PATH = "/storefront/user-addresses";
    private static final String USER_ADDRESS_ID_PATH = "/storefront/user-address/{id}";
    private static final String USER_ADDRESSES_ID_PATH = "/storefront/user-addresses/{id}";
    private static final String DEFAULT_ADDRESS_PATH = "/storefront/user-address/default-address";
    private static final String DEFAULT_ADDRESSES_PATH = "/storefront/user-addresses/default-address";

    private final UserAddressService userAddressService;

    @GetMapping({USER_ADDRESS_PATH, USER_ADDRESSES_PATH})
    public ResponseEntity<List<ActiveAddressVm>> getUserAddresses() {
        return ResponseEntity.ok(userAddressService.getUserAddressList());
    }

    @GetMapping({DEFAULT_ADDRESS_PATH, DEFAULT_ADDRESSES_PATH})
    public ResponseEntity<AddressDetailVm> getDefaultAddress() {
        return userAddressService.findAddressDefault()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping({USER_ADDRESS_PATH, USER_ADDRESSES_PATH})
    public ResponseEntity<UserAddressVm> createAddress(@Valid @RequestBody AddressPostVm addressPostVm) {
        return ResponseEntity.ok(userAddressService.createAddress(addressPostVm));
    }

    @DeleteMapping({USER_ADDRESS_ID_PATH, USER_ADDRESSES_ID_PATH})
    public ResponseEntity deleteAddress(@PathVariable Long id) {
        userAddressService.deleteAddress(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping({USER_ADDRESS_ID_PATH, USER_ADDRESSES_ID_PATH})
    public ResponseEntity chooseDefaultAddress(@PathVariable Long id) {
        userAddressService.chooseDefaultAddress(id);
        return ResponseEntity.ok().build();
    }
}
