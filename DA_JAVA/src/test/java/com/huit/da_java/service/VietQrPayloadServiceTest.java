package com.huit.da_java.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VietQrPayloadServiceTest {

    private final VietQrPayloadService service = new VietQrPayloadService();

    @Test
    void buildMomoTransferPayloadIncludesAmountAndOrderContent() {
        String payload = service.buildMomoTransferPayload(25000, "DON42");

        assertTrue(payload.contains("010212"));
        assertTrue(payload.contains("540525000"));
        assertTrue(payload.contains("0805DON42"));
        assertTrue(payload.contains("0515MOMOW2W87256599"));
        assertTrue(payload.endsWith(crc16(payload.substring(0, payload.length() - 4))));
    }

    @Test
    void buildMomoTransferPayloadReplacesOldAmountAndContent() {
        String first = service.buildMomoTransferPayload(25000, "DON42");
        String second = service.buildMomoTransferPayload(120000, "DON99");

        assertTrue(second.contains("5406120000"));
        assertTrue(second.contains("0805DON99"));
        assertTrue(!second.contains("540525000"));
        assertEquals(1, count(second, "DON99"));
        assertTrue(!second.equals(first));
    }

    private int count(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
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
}
