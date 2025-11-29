package com.infy.pinterest.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PinVisibilityConverter implements AttributeConverter<Pin.Visibility, String> {
    
    @Override
    public String convertToDatabaseColumn(Pin.Visibility attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public Pin.Visibility convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return Pin.Visibility.valueOf(dbData.toUpperCase());
    }
}
