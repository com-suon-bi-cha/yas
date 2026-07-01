package com.yas.tax.viewmodel.taxclass;

import com.yas.tax.model.TaxClass;

// CI pipeline test trigger
public record TaxClassVm(Long id, String name) {

    public static TaxClassVm fromModel(TaxClass taxClass) {
        return new TaxClassVm(taxClass.getId(), taxClass.getName());
    }
}
