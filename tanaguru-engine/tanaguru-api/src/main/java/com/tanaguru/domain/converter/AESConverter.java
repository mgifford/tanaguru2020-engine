package com.tanaguru.domain.converter;

import com.tanaguru.config.PropertyConfig;
import com.tanaguru.helper.AESEncrypt;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;;

@Converter
public class AESConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String message) {
        if(message == null){
            return null;
        }
        return AESEncrypt.encrypt(message, PropertyConfig.cryptoKey);
    }

    @Override
    public String convertToEntityAttribute(String encryptedMessage) {
        if(encryptedMessage == null){
            return null;
        }
        return AESEncrypt.decrypt(encryptedMessage, PropertyConfig.cryptoKey);
    }
}
