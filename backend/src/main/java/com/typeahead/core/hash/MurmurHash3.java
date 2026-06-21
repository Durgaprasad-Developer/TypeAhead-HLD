package com.typeahead.core.hash;

import java.nio.charset.StandardCharsets;

/**
 * Standard implementation of 32-bit MurmurHash3.
 *
 * <p>Used in consistent hashing to hash query prefixes and node identifiers
 * uniformly across the hash ring.
 */
public final class MurmurHash3 {

    private MurmurHash3() {}

    /**
     * Hash a string to a 32-bit signed integer.
     *
     * @param data The input string.
     * @return The 32-bit hash.
     */
    public static int hash32(String data) {
        if (data == null) {
            return 0;
        }
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        return hash32(bytes, bytes.length, 0);
    }

    /**
     * Standard MurmurHash3 32-bit implementation.
     * Source: https://en.wikipedia.org/wiki/MurmurHash
     */
    public static int hash32(byte[] data, int length, int seed) {
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;

        int h1 = seed;
        int roundedEnd = (length & 0xfffffffc); // round down to 4 byte block

        for (int i = 0; i < roundedEnd; i += 4) {
            // little endian load
            int k1 = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | (data[i + 3] << 24);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        // tail
        int k1 = 0;
        int val = length & 0x03;
        if (val == 3) {
            k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
        }
        if (val >= 2) {
            k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
        }
        if (val >= 1) {
            k1 ^= (data[roundedEnd] & 0xff);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            h1 ^= k1;
        }

        // finalisation
        h1 ^= length;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);

        return h1;
    }
}
