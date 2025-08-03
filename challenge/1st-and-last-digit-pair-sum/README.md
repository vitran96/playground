# First-and-Last Digit Pair Sum

Company: NAB

## Description

Given an array of positive two-digit integers, find the maximum sum you can get by picking two numbers that share the same first and last digit.  
If no such pair exists, return `0`.

### Example:

```plaintext
[120, 23, 13, 33, 92, 10, 100, 103]

// (120, 10, 100) share first=1, last=0 → max couple is 120 vs 100
// => 120+100 = 220

// (13, 103) share first=1, last=3 → max couple is 13 vs 103
// => 13+103 = 116

Answer: max(220, 116) = 220
