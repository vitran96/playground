
TEST_CASES = [
    {
        'input': "(2x+3)(4x-5)",
        'expected': "8x^2+2x-15"
    },
    {
        'input': "(-1x+2)(3x+4)",
        'expected': "-3x^2+2x+8"
    },
    {
        'input': "(2x+3)(4x+5)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(-3+2x)(-4x+5)",
        'expected': "-8x^2+22x-15"
    },
    {
        'input': "(-2x-3)(-5-4x)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(-3-2x)(-5-4x)",
        'expected': "8x^2+22x+15"
    },
    {
        'input': "(x+1)(x+1)",
        'expected': "x^2+2x+1"
    },
    # {
    #      # NOTE: special case
    #     'input': "(3x+0)(0x+5)",
    #     'expected': "0"
    # },
]

def str_2_int(s):
    if s == '':
        return 1
    return int(s)

# Assume that this will always valid
def extract_nums(_str):
    l = 0
    r = 0
    items = _str.split('x')
    # print(items)
    if items[1] == '':
        # x on the right
        arr = list(items[0])
        l_str = ''
        i = len(arr) - 1
        # print(arr)
        while arr[i] != '+' and arr[i] != '-':
            l_str = arr[i] + l_str
            i = i - 1
        l_str = arr[i] + l_str
        # print(l_str)
        l = str_2_int(l_str)
        # print(''.join(arr[0:i]))
        r_str = ''.join(arr[0:i])
        r = str_2_int(r_str)
    else:
        # len(items) == 2
        # x on the left
        l = str_2_int(items[0])
        r = str_2_int(items[1])
    return l, r

def calculate_nums(a, b, c, d):
    # print(a, b, c, d)
    new_a = a*c
    new_b = a*d + b*c
    new_c = b*d
    return new_a, new_b, new_c

def construct_str(n, prefix, postfix):
    result = ''

    if n > 1:
        result = f'{prefix}{n}{postfix}'
    elif n < -1:
        result = f'{n}{postfix}'
    elif n == 1:
        if postfix == '':
            postfix = '1'
        result = f'{prefix}{postfix}'
    elif n == -1:
        if postfix == '':
            postfix = '1'
        result = f'{postfix}'
    
    return result

def construct_formula(a, b, c):
    # print(a, b, c)
    result = construct_str(a, '', 'x^2') + construct_str(b, '+', 'x') + construct_str(c, '+', '')
    if result == '':
        return '0'
    return result

# Eg: (2x+3)(4x-5) -> 8x^2+2x-15
# (-1x+2)(3x+4) → -3x^2+2x+8
# assume a, b, c, d == 0 is special edge case
# assume a, b, c, d s' digit >= 0 (no digit == 1)
# assume no further nesting bracket
def expand(_str):
    # potential approach, identify a, b, c, d
    # then new a' = a*c, b' = (a*c + b*d), c' = b*d

    # Naive approach
    # 1. Split by )
    # 2. Remove (
    # 3. For each
    #   3.1. split by x
    #   3.2.a If has 2 items, left is a / c and right is b / d
    #   3.2.b else, x likely on the right size, then everything for the right to the 1st +/- is a / c and the rest is b / d
    # 4. now, with a, b, c, d => calculate a', b', c'
    # 5. If a' / b' / c' == 0 -> ommit that one along with it's x
    # 6. Elif a' / b' / c' == 1 -> ommit the number
    items = [item.replace('(', '').strip() for item in _str.split(')')]

    if len(items) < 2:
        # Invalid formula
        # Assume that this won't happen
        return ''

    # print(items)
    a, b = extract_nums(items[0])
    c, d = extract_nums(items[1])
    new_a, new_b, new_c = calculate_nums(a, b, c, d)
    return construct_formula(new_a, new_b, new_c)

for case in TEST_CASES:
    result = False
    _input = case['input']
    expected = case['expected']
    try:

        actual = expand(_input)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
