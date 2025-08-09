# Binomial Expansion (HARD)

Company: OL

## Description

Have the function `MathChallenge(str)` take `str`, which will be a string representing a polynomial containing only:
- (+/-) integers
- a letter
- parenthesis
- the symbol `^`
and return it in **expanded form**.

---

### ✅ Example:

If the input is: `"(2x^2+4)(6x^3+3)^2"`
The output should be: `12x^5+24x^3+6x^2+12`


---

- Both the input and output should contain **no spaces**.
- The input will only contain **one letter**, such as `"x"`, `"y"`, etc.
- There will only be **four parentheses** in the input.
- The output should contain **no parentheses**.
- The output should be returned with the **highest exponential element first**, down to the lowest.

---

### 🧾 Grammar (Expression Pattern):

The general form of `str` will be: `([+/-]{num}[{letter}[{^}[+/-]{num}]]...[[+/-]{num}])(copy)`

Where:
- `{}` = optional parts
- `()` = mandatory groups
- `num` = integer
- `letter` = variable symbol like `x`

---

### Examples

```plaintext
Input: "(x+1)(x^2-1)"
Output: x^3+x^2+x-1
```

