package com.foodgroup.common.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return aesEncryptor.decrypt(dbData);
    }
}
