# BinaryGap

Find longest sequence of zeros in binary representation of an integer.

---

A **binary gap** within a positive integer `N` is any maximal sequence of consecutive zeros that is surrounded by ones at both ends in the binary representation of `N`.

For example:
- Number `9` has binary representation `1001` and contains a binary gap of length **2**.
- Number `529` has binary representation `1000010001` and contains two binary gaps: one of length **4** and one of length **3**.
- Number `20` has binary representation `10100` and contains one binary gap of length **1**.
- Number `15` has binary representation `1111` and has **no** binary gaps.
- Number `32` has binary representation `100000` and has **no** binary gaps.
