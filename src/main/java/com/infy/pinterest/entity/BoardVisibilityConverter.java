package com.infy.pinterest.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BoardVisibilityConverter implements AttributeConverter<Board.Visibility, String> {
    
    @Override
    public String convertToDatabaseColumn(Board.Visibility attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public Board.Visibility convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return Board.Visibility.valueOf(dbData.toUpperCase());
    }
}
