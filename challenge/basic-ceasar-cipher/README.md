# Basic Caesar Cipher

Company: OL

## Description

Write a function `caesar(str, n)` that shifts each letter in str forward by n places in the alphabet (wrapping around from “z” to “a”), **preserving case; all non-letters stay unchanged.**

Once your function is working, take the final output string and remove any characters _(case-insensitive)_ from it that appear in your **ChallengeToken**. If the new final string is empty, return the string.

Your ChallengeToken: `ju51abeir73`
Examples

```
Input: "Hello" & num = 4
Output: Lipps
Final Output: Lpps
```

```
Input: "abc" & num = 0
Output: abc
Final Output: c
```