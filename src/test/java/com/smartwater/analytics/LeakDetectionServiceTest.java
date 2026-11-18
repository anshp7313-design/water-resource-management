package com.smartwater.analytics;

import com.smartwater.model.LeakAlert;
import com.smartwater.model.WaterRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeakDetectionServiceTest {
    @Test
    void detectsCriticalSpikeAboveBaselineAndLimit() {
        LeakDetectionService service = LeakDetectionService.getInstance();
        List<WaterRecord> records = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 5, 1);
        for (int i = 0; i < 7; i++) {
            records.add(new WaterRecord(start.plusDays(i), 100, ""));
        }
        records.add(new WaterRecord(start.plusDays(7), 260, ""));

        LeakAlert alert = service.analyzeRecentUsage(records, 200);

        assertEquals(LeakAlert.Severity.CRITICAL, alert.getSeverity());
        assertEquals(start.plusDays(7), alert.getDate());
    }
}
