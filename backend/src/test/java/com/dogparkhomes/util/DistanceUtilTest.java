package com.dogparkhomes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistanceUtilTest {

    @Test
    void haversineMiles_samePoint_isZero() {
        assertEquals(0.0, DistanceUtil.haversineMiles(0, 0, 0, 0), 0.0);
        assertEquals(0.0, DistanceUtil.haversineMiles(37.7749, -122.4194, 37.7749, -122.4194), 0.0);
    }

    @Test
    void haversineMiles_isSymmetric() {
        double aToB = DistanceUtil.haversineMiles(37.7749, -122.4194, 34.0522, -118.2437); // SF -> LA
        double bToA = DistanceUtil.haversineMiles(34.0522, -118.2437, 37.7749, -122.4194); // LA -> SF
        assertEquals(aToB, bToA, 0.0);
    }

    @Test
    void haversineMiles_sanFranciscoToLosAngeles_isReasonable() {
        // Great-circle distance SF<->LA is ~347 miles; our util rounds to 2 decimals.
        double miles = DistanceUtil.haversineMiles(37.7749, -122.4194, 34.0522, -118.2437);
        assertTrue(miles > 330 && miles < 380, "Expected SF->LA miles to be within a reasonable range, got " + miles);
    }
}

