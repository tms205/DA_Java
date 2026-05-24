package com.huit.da_java.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VietQrPayloadService {
    private static final String BASE_MOMO_QR = "00020101021138620010A00000072701320006970454011899MM24196M872565990208QRIBFTTA53037045802VN62190515MOMOW2W8725659963045CFA";

    public String buildMomoTransferPayload(long amount, String content) {
        List<Tlv> fields = parse(stripCrc(BASE_MOMO_QR));
        set(fields, "01", "12");
        setAfter(fields, "53", "54", String.valueOf(amount));
        set(fields, "62", buildAdditionalData(content));

        String withoutCrc = encode(fields) + "6304";
        return withoutCrc + crc16(withoutCrc);
    }

    private String buildAdditionalData(String content) {
        List<Tlv> fields = parse(find(parse(stripCrc(BASE_MOMO_QR)), "62"));
        remove(fields, "08");
        fields.add(new Tlv("08", content));
        return encode(fields);
    }

    private String stripCrc(String payload) {
        int crcIndex = payload.lastIndexOf("6304");
        if (crcIndex < 0) {
            return payload;
        }
        return payload.substring(0, crcIndex);
    }

    private List<Tlv> parse(String payload) {
        List<Tlv> fields = new ArrayList<>();
        int index = 0;
        while (index + 4 <= payload.length()) {
            String tag = payload.substring(index, index + 2);
            int length = Integer.parseInt(payload.substring(index + 2, index + 4));
            int valueStart = index + 4;
            int valueEnd = valueStart + length;
            if (valueEnd > payload.length()) {
                break;
            }
            fields.add(new Tlv(tag, payload.substring(valueStart, valueEnd)));
            index = valueEnd;
        }
        return fields;
    }

    private String encode(List<Tlv> fields) {
        StringBuilder builder = new StringBuilder();
        for (Tlv field : fields) {
            builder.append(field.tag())
                    .append("%02d".formatted(field.value().length()))
                    .append(field.value());
        }
        return builder.toString();
    }

    private String find(List<Tlv> fields, String tag) {
        return fields.stream()
                .filter(field -> field.tag().equals(tag))
                .map(Tlv::value)
                .findFirst()
                .orElse("");
    }

    private void set(List<Tlv> fields, String tag, String value) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).tag().equals(tag)) {
                fields.set(i, new Tlv(tag, value));
                return;
            }
        }
        fields.add(new Tlv(tag, value));
    }

    private void setAfter(List<Tlv> fields, String afterTag, String tag, String value) {
        remove(fields, tag);
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).tag().equals(afterTag)) {
                fields.add(i + 1, new Tlv(tag, value));
                return;
            }
        }
        fields.add(new Tlv(tag, value));
    }

    private void remove(List<Tlv> fields, String tag) {
        fields.removeIf(field -> field.tag().equals(tag));
    }

    private String crc16(String payload) {
        int crc = 0xFFFF;
        for (int i = 0; i < payload.length(); i++) {
            crc ^= payload.charAt(i) << 8;
            for (int bit = 0; bit < 8; bit++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return "%04X".formatted(crc);
    }

    private record Tlv(String tag, String value) {
    }
}
