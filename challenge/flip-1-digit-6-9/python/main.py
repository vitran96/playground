TEST_CASES = [
    {
        'input': 9669,
        'expected': 9969
    },
    {
        'input': 9996,
        'expected': 9999
    },
    {
        'input': 6666,
        'expected': 9666
    },
    {
        'input': 9999,
        'expected': 9999
    },
    {
        'input': 6969,
        'expected': 9969
    },
]

def flip_1_digit_6_9(n):
    # in my opinion, we only need to flip the most left '6' in the number
    arr = list(str(n))
    result_arr = []

    flipped = False
    for num in arr:
        if num == '6' and not flipped:
            num = '9'
            flipped = True

        result_arr.append(num)

    return int(''.join(result_arr))

for case in TEST_CASES:
    result = False
    _input = case['input']

    expected = case['expected']
    try:
        actual = flip_1_digit_6_9(_input)

        result = expected == actual

        print(_input, ' -> ', expected, ' == ', actual, ' = ', result)
    except Exception as e:
        print(_input, ' -> ', 'Error: ', e)
