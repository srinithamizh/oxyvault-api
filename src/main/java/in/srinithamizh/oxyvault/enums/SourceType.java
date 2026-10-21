package in.srinithamizh.oxyvault.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum SourceType {
    BANK,
    CARD,
    CASH,
    LOAN;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static SourceType fromJson(String value) {
        return value == null ? null : valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
