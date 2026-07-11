package com.yas.tax.viewmodel.taxclass;

import com.yas.tax.model.TaxClass;

// ViewModel for TaxClass — used in API responses (v2)
public record TaxClassVm(Long id, String name) {

    public static TaxClassVm fromModel(TaxClass taxClass) {
        return new TaxClassVm(taxClass.getId(), taxClass.getName());
    }
}
// trigger Wed Jul  1 20:43:29 +07 2026
