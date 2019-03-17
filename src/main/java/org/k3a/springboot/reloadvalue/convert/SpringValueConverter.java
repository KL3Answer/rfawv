package org.k3a.springboot.reloadvalue.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.io.IOException;

/**
 * Created by k3a
 * on 2019/2/27  PM 9:00
 * <p>
 */
@SuppressWarnings("WeakerAccess")
public class SpringValueConverter implements ValueConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringValueConverter.class);

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public final ConversionService conversionService;

    public SpringValueConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public <T> T convert(Class<T> type, String text) {
        // use type converter
        if (conversionService.canConvert(String.class, type)) {
            return conversionService.convert(text, type);
        } else {
            // from constructor , valueOf or from json
            try {
                return OBJECT_MAPPER.readValue(text, type);
            } catch (IOException e) {
                LOGGER.error("ObjectMapper readValue error:", e);
                throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName());
            }
        }

    }
}
